# ZenToDo-api

This is the API for interacting with the ZenToDo server (https://github.com/berndreiss/ZenToDo-server). It also contains all the relevant shared data types necessary to implement a ZenToDo list.

It provides:
* A client stub for interaction with the server
* Data classes and interfaces for database management:
  * Classes for data types (implementing JPA):
    *  Entry
    *  TaskList
    *  User
    *  etc.
  * An abstract database class consisting of
    * An interface for an entry manager
    * An interface for a user manager
    * An interface for a list manager
    * An interface for general database operations
