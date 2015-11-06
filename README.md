Writable Streams
================
A project which adds writable streams that can be used to, for example,
to transform other streams by providing a simple writer interface which
can then be read from. This project was created and is managed by
Donald Blodgett.

Usage
-----
First, create an implementation of either a OutputHandler or WriteHandler,
depending on the type of stream you wish to work with. Then provide an
instance to the corresponding OutputableInputStream or WritableReader.
Finally read from the stream as you would any other stream.