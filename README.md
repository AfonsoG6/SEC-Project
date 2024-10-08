# BFT Banking (BFTB)

Highly Dependable Systems 2021-2022, 2nd semester, 1st period project.

## Authors

**Group 35**

- Afonso Gomes - [92410](mailto:afonso.gomes@tecnico.ulisboa.pt)

- Manuel Carvalho - [87237](mailto:manuel.g.carvalho@tecnico.ulisboa.pt)

- Manuel Domingues - [82437](mailto:manuel.domingues@tecnico.ulisboa.pt)

## Getting Started

The overall system is composed of multiple modules:

- **client**: the module that is responsible for the client API and the user interface.
- **server**: the module that is responsible for the server infrastructure.
- **contract**: the module that defines the contract between the client and the server, i.e. the remote procedures
  supported by the system.

### Prerequisites

Java Developer Kit 17 is required running on Linux, Windows or Mac.

Maven 3 is also required.

To confirm that you have them installed, open a terminal and type:

```shell
javac -version

mvn -version
```

### Installing

To compile and install all modules:

```shell
mvn clean install -DskipTests
```

*Note:* Alternatively, you can compile and install each module separately, by using the `-pl` option:

```shell
mvn clean install -DskipTests -pl <module>
```

### Running

***Note:*** To use a different **f** value from the default value of **1**, you must run the script `cert_gen.sh` to
generate the certificates for the necessary number of replicas.

You must also add `-Df=<f>` to the end of all the following `exec:java` commands.

#### Running the server

To run the server with the default configuration, open a terminal and type:

```shell
mvn exec:java -pl server
```

You may specify the port number to use, by using the `-Dsvport=<port>` option.

To run multiple replicas of the server, you must use the `-Dn=<number>` option. Where number must be a number between **
0 and 3f**.

#### Running the client

Then to run a client, open a terminal and type:

```shell
mvn exec:java -pl client
```

You may specify the server address and port number to use, by using the `-Dsvhost=<hostname>` and `-Dsvport=<port>`
options.

*Note:* If the intended hostname contains a `.` (dot), you must use quotes around it (ex: `-Dsvhost="127.0.0.1"`).

### Running the demo

To run the included demo, you must first run the server replicas, and then open a terminal and type:

```shell
mvn exec:java -pl client -Dinputfile="demo.txt"
```

## Built With

* [Maven](https://maven.apache.org/) - Build Tool and Dependency Management
* [gRPC](https://grpc.io/) - RPC framework