/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import org.slf4j.LoggerFactory

import scala.collection.immutable.BitSet
import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.CharSequenceReader

/**
 * The parser for cache directives specified in the "Cache-Control" HTTP header.
 *
 * https://tools.ietf.org/html/rfc7234#section-5.2
 */
object CacheDirectiveParser {

  def parse(headers: Seq[String]): collection.immutable.Seq[CacheDirective] = {
    CacheDirectiveParserCompat.toImmutableSeq(headers.flatMap(parse))
  }

  def parse(header: String): collection.immutable.Seq[CacheDirective] = {
    CacheControlParser(new CharSequenceReader(header)) match {
      case CacheControlParser.Success(result, next) =>
        result
      case _: CacheControlParser.NoSuccess =>
        Nil
    }
  }

  object CacheControlParser extends Parsers {
    // http://www.donroby.com/wp/scala/parsing-expressions-with-scala-parser-combinators-2/
    // http://bitwalker.org/blog/2013/08/10/learn-by-example-scala-parser-combinators/
    // https://wiki.scala-lang.org/display/SW/Parser+Combinators--Getting+Started
    // http://www.bizcoder.com/everything-you-need-to-know-about-http-header-syntax-but-were-afraid-to-ask

    import CacheDirectives._

    private val logger = LoggerFactory.getLogger("com.typesafe.cachecontrol.CacheControlParser")

    val separatorChars = "()<>@,;:\\\"/[]?={} \t"
    val separatorBitSet: BitSet = BitSet(separatorChars.toIndexedSeq.map(_.toInt): _*)

    type Elem = Char

    val any = acceptIf(_ => true)(_ => "Expected any character")
    val end = not(any)

    // https://tools.ietf.org/html/rfc7231
    /*
     * RFC 2616 section 2.2
     *
     * These patterns are translated directly using the same naming
     */
    val ctl = acceptIf { c =>
      (c >= 0 && c <= 0x1F) || c.toInt == 0x7F
    }(_ => "Expected a control character")
    val char = acceptIf(_ < 0x80)(_ => "Expected an ascii character")
    val text = not(ctl) ~> any
    val separators = {
      acceptIf(c => separatorBitSet(c.toInt))(_ => "Expected one of " + separatorChars)
    }

    // token = <token, see [RFC7230], Section 3.2.6>
    val token = rep1(not(separators | ctl) ~> any) ^^ charSeqToString

    def badPart(p: Char => Boolean, msg: => String): Parser[None.type] = rep1(acceptIf(p)(ignoreErrors)) ^^ {
      case chars =>
        logger.debug(msg + ": " + charSeqToString(chars))
        None
    }

    val badParameter = badPart(c => c != ',' && c != ';', "Bad parameter")

    def tolerant[T](p: Parser[T], bad: Parser[Option[T]]): Parser[Option[T]] = p.map(Some.apply) | bad

    // The spec is really vague about what a quotedPair means. We're going to assume that it's just to quote quotes,
    // which means all we have to do for the result of it is ignore the slash.
    val quotedPair = '\\' ~> char
    val qdtext = not('"') ~> text

    // quoted-string https://tools.ietf.org/html/rfc7230#section-3.2.6
    val quotedString = '"' ~> rep(quotedPair | qdtext) <~ '"' ^^ charSeqToString

    def seconds(n: String): Seconds = {
      val unquoted = s"PT${n}S"
      Seconds.parse(unquoted)
    }

    //   cache-directive = token [ "=" ( token / quoted-string ) ]

    /*
     Cache directives are identified by a token, to be compared
     case-insensitively, and have an optional argument, that can use both
     token and quoted-string syntax.  For the directives defined below
     that define arguments, recipients ought to accept both forms, even if
     one is documented to be preferred.  For any directive not defined by
     this specification, a recipient MUST accept both forms.

     Per http://tools.ietf.org/html/rfc7234#appendix-A, cache directives are
     explicitly defined to be case-insensitive.
     */
    val cacheDirective: Parser[CacheDirective] = token ~ opt('=' ~> (token | quotedString)) <~ rep(' ') ^^ {
      case maxAge ~ Some(v) if maxAge matches "(?i)max-age" =>
        val delta = seconds(v)
        MaxAge(delta)

      case maxStale ~ None if maxStale matches "(?i)max-stale" =>
        MaxStale(None)

      case maxStale ~ Some(v) if maxStale matches "(?i)max-stale" =>
        val delta = seconds(v)
        MaxStale(Some(delta))

      case minFresh ~ Some(v) if minFresh matches "(?i)min-fresh" =>
        val delta = seconds(v)
        MinFresh(delta)

      case noCache ~ Some(v) if noCache matches "(?i)no-cache" =>
        val args = fieldNames(v)
        NoCache(Some(args))

      case noCache ~ None if noCache matches "(?i)no-cache" =>
        NoCache(None)

      case noStore ~ None if noStore matches "(?i)no-store" =>
        NoStore

      case noTransform ~ None if noTransform matches "(?i)no-transform" =>
        NoTransform

      case onlyIfCached ~ None if onlyIfCached matches "(?i)only-if-cached" =>
        OnlyIfCached

      case mustRevalidated ~ None if mustRevalidated matches "(?i)must-revalidate" =>
        MustRevalidate

      case public ~ None if public matches "(?i)public" =>
        Public

      case privateDirective ~ Some(v) if privateDirective matches "(?i)private" =>
        val args = fieldNames(v)
        Private(Some(args))

      case privateDirective ~ None if privateDirective matches "(?i)private" =>
        Private(None)

      case proxyRevalidate ~ None if proxyRevalidate matches "(?i)proxy-revalidate" =>
        ProxyRevalidate

      case staleWhileRevalidate ~ Some(v) if staleWhileRevalidate matches "(?i)stale-while-revalidate" =>
        val delta = seconds(v)
        StaleWhileRevalidate(delta)

      case staleIfError ~ Some(v) if staleIfError matches "(?i)stale-if-error" =>
        val delta = seconds(v)
        StaleIfError(delta)

      case sMaxAge ~ Some(v) if sMaxAge matches "(?i)s-maxage" =>
        val delta = seconds(v)
        SMaxAge(delta)

      case name ~ value =>
        CacheDirectiveExtension(name, value)
    }

    // Either it's a valid parameter followed immediately by the end, a comma or a semicolon, or it's a bad parameter
    val tolerantParameter = tolerant(cacheDirective <~ guard(end | ';' | ','), badParameter)

    val parameters = rep(';' ~> rep(' ') ~> tolerantParameter <~ rep(' '))

    // OWS -- optional whitespace as defined in https://tools.ietf.org/html/rfc7230#section-3.2.3
    // https://tools.ietf.org/html/rfc7234#section-1.2
    // https://tools.ietf.org/html/rfc7230#section-7

    // https://tools.ietf.org/html/rfc7234#appendix-C
    //  Cache-Control = *( "," OWS ) cache-directive *( OWS "," [ OWS cache-directive ] )
    val cacheDirectives: CacheControlParser.Parser[collection.immutable.Seq[CacheDirective]] = {
      rep1sep(cacheDirective, ',' ~ rep(' '))
    }

    def apply(in: Input): ParseResult[collection.immutable.Seq[CacheDirective]] = {
      cacheDirectives(in)
    }

    def charSeqToString(chars: Seq[Char]): String = new String(chars.toArray)

    def ignoreErrors(c: Char): String = ""
  }

  def fieldNames(v: String): List[String] = {
    v.split(",").map(_.trim).toList
  }
}
