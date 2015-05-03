# HTTP Headers

This is a minimal library that provides a set of utility calculators that abstract away much of the fiddly complexities involved in adhering to the HTTP caching model, aka RFC 7234.

It does not implement caching itself, and there are some aspects of RFC 7234 which must be implemented directly, such as stripping headers, invalidating unsafe methods, and implementing the Vary header functionality correctly.


