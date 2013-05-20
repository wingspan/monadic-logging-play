monadic-logging-play
====================

a proof-of-concept Scala Play webapp using monadic logging around I/O that can fail. Uses scalaz and monad transformers.

## motivation

Using a traditional mutable logger singleton means that logs from concurrent requests are interleaved. If we were to
pass logs up the call stack, rather than mutating a global, we have access to the complete and isolated logs across the
duration of a request, even in the presence of errors.

We use a monad similar to `Writer[List[String], Either[Throwable, A]]` to achieve this.

## how to run

Does not require Play installed; SBT will download play along with other dependencies.

From the project root: `sbt play run`

## License

Distributed under the MIT License.

Contributors:

* Dustin Getz
* Martin Snyder
