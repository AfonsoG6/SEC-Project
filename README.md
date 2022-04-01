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

#### Running the server

To run the server, open a terminal and type:

```shell
mvn exec:java -pl server
```

You may specify the port number to use, by using the `-Dsvport=<port>` option.

#### Running the client

Then to run a client, open a terminal and type:

```shell
mvn exec:java -pl client
```

You may specify the server address and port number to use, by using the `-Dsvhost=<hostname>` and `-Dsvport=<port>`
options.

*Note:* If the intended hostname contains a `.` (dot), you must use quotes around it (ex: `-Dsvhost="127.0.0.1"`).

### Running the tests

To run the existing tests, first you must run the server:

```shell
mvn exec:java -pl server
```

Then, open another terminal and type:

```shell
mvn verify
```

### Running the demo

To run the included demo, you must first run the server:

```shell
mvn exec:java -pl server
```

Then, open another terminal and type:

```shell
mvn exec:java -pl client -Dinputfile="demo.txt"
```

The following output should be displayed:

```
--------------------------------------------------------------------------------
> chuser afonso
User changed to 'afonso'
--------------------------------------------------------------------------------
> open
Operation successful!
--------------------------------------------------------------------------------
> check
Operation successful!
Balance: 100
Pending Transfers:

--------------------------------------------------------------------------------
> audit
Operation successful!
Transaction History:

--------------------------------------------------------------------------------
> chuser manuel
User changed to 'manuel'
--------------------------------------------------------------------------------
> open
Operation successful!
--------------------------------------------------------------------------------
> check
Operation successful!
Balance: 100
Pending Transfers:

--------------------------------------------------------------------------------
> audit
Operation successful!
Transaction History:

--------------------------------------------------------------------------------
> send afonso 10
Operation successful!
--------------------------------------------------------------------------------
> audit
Operation successful!
Transaction History:
OUTGOING TRANSFER no.0: [Pending] $ 10 from MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6aODKvT7Alv5DhktdrgWIfsAQrwrTEPMkvggP7aYPiP2
p0DSC06I0tn6YHtr0xbB/pPXoSJv785wID5e/7mU96R69UUnbLvc5fRWVcdMP23jrECqJxSmVMYfmD9ag2E9cVkbgwTnWwIpSKg0Ie1hqroDqMwqBFlKjWizgcp2qzTp4xZh
fOoxd98OacuRDEArgaH6uNUKDIc8Ef5JWzB/o443ZzF5TCl2mWejHiRQwbtm54BIZx5FF50ml0iLtYGXXqW8D4MxSs2L9zWy1Ydow9ep85DB+vNLp7ujd8Ta4QhmnS6y1S8d
JUoe1ZRETEzjMRsApHP+c5UAcpIGb84YlwIDAQAB to MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6aODKvT7Alv5DhktdrgWIfsAQrwrTEPMkvggP7aYPiP2
p0DSC06I0tn6YHtr0xbB/pPXoSJv785wID5e/7mU96R69UUnbLvc5fRWVcdMP23jrECqJxSmVMYfmD9ag2E9cVkbgwTnWwIpSKg0Ie1hqroDqMwqBFlKjWizgcp2qzTp4xZh
fOoxd98OacuRDEArgaH6uNUKDIc8Ef5JWzB/o443ZzF5TCl2mWejHiRQwbtm54BIZx5FF50ml0iLtYGXXqW8D4MxSs2L9zWy1Ydow9ep85DB+vNLp7ujd8Ta4QhmnS6y1S8d
JUoe1ZRETEzjMRsApHP+c5UAcpIGb84YlwIDAQAB

--------------------------------------------------------------------------------
> chuser afonso
User changed to 'afonso'
--------------------------------------------------------------------------------
> check
Operation successful!
Balance: 100
Pending Transfers:
INCOMING TRANSFER no.0: [Pending] $ 10 from MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6aODKvT7Alv5DhktdrgWIfsAQrwrTEPMkvggP7aYPiP2
p0DSC06I0tn6YHtr0xbB/pPXoSJv785wID5e/7mU96R69UUnbLvc5fRWVcdMP23jrECqJxSmVMYfmD9ag2E9cVkbgwTnWwIpSKg0Ie1hqroDqMwqBFlKjWizgcp2qzTp4xZh
fOoxd98OacuRDEArgaH6uNUKDIc8Ef5JWzB/o443ZzF5TCl2mWejHiRQwbtm54BIZx5FF50ml0iLtYGXXqW8D4MxSs2L9zWy1Ydow9ep85DB+vNLp7ujd8Ta4QhmnS6y1S8d
JUoe1ZRETEzjMRsApHP+c5UAcpIGb84YlwIDAQAB to MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6aODKvT7Alv5DhktdrgWIfsAQrwrTEPMkvggP7aYPiP2
p0DSC06I0tn6YHtr0xbB/pPXoSJv785wID5e/7mU96R69UUnbLvc5fRWVcdMP23jrECqJxSmVMYfmD9ag2E9cVkbgwTnWwIpSKg0Ie1hqroDqMwqBFlKjWizgcp2qzTp4xZh
fOoxd98OacuRDEArgaH6uNUKDIc8Ef5JWzB/o443ZzF5TCl2mWejHiRQwbtm54BIZx5FF50ml0iLtYGXXqW8D4MxSs2L9zWy1Ydow9ep85DB+vNLp7ujd8Ta4QhmnS6y1S8d
JUoe1ZRETEzjMRsApHP+c5UAcpIGb84YlwIDAQAB

--------------------------------------------------------------------------------
> recv
Operation successful!
--------------------------------------------------------------------------------
> check
Operation successful!
Balance: 110
Pending Transfers:

--------------------------------------------------------------------------------
> audit
Operation successful!
Transaction History:
INCOMING TRANSFER no.0: [Approved] $ 10 from MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6aODKvT7Alv5DhktdrgWIfsAQrwrTEPMkvggP7aYPiP
2p0DSC06I0tn6YHtr0xbB/pPXoSJv785wID5e/7mU96R69UUnbLvc5fRWVcdMP23jrECqJxSmVMYfmD9ag2E9cVkbgwTnWwIpSKg0Ie1hqroDqMwqBFlKjWizgcp2qzTp4xZ
hfOoxd98OacuRDEArgaH6uNUKDIc8Ef5JWzB/o443ZzF5TCl2mWejHiRQwbtm54BIZx5FF50ml0iLtYGXXqW8D4MxSs2L9zWy1Ydow9ep85DB+vNLp7ujd8Ta4QhmnS6y1S8
dJUoe1ZRETEzjMRsApHP+c5UAcpIGb84YlwIDAQAB to MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6aODKvT7Alv5DhktdrgWIfsAQrwrTEPMkvggP7aYPiP
2p0DSC06I0tn6YHtr0xbB/pPXoSJv785wID5e/7mU96R69UUnbLvc5fRWVcdMP23jrECqJxSmVMYfmD9ag2E9cVkbgwTnWwIpSKg0Ie1hqroDqMwqBFlKjWizgcp2qzTp4xZ
hfOoxd98OacuRDEArgaH6uNUKDIc8Ef5JWzB/o443ZzF5TCl2mWejHiRQwbtm54BIZx5FF50ml0iLtYGXXqW8D4MxSs2L9zWy1Ydow9ep85DB+vNLp7ujd8Ta4QhmnS6y1S8
dJUoe1ZRETEzjMRsApHP+c5UAcpIGb84YlwIDAQAB

--------------------------------------------------------------------------------
> chuser manuel
User changed to 'manuel'
--------------------------------------------------------------------------------
> audit
Operation successful!
Transaction History:
OUTGOING TRANSFER no.0: [Approved] $ 10 from MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6aODKvT7Alv5DhktdrgWIfsAQrwrTEPMkvggP7aYPiP
2p0DSC06I0tn6YHtr0xbB/pPXoSJv785wID5e/7mU96R69UUnbLvc5fRWVcdMP23jrECqJxSmVMYfmD9ag2E9cVkbgwTnWwIpSKg0Ie1hqroDqMwqBFlKjWizgcp2qzTp4xZ
hfOoxd98OacuRDEArgaH6uNUKDIc8Ef5JWzB/o443ZzF5TCl2mWejHiRQwbtm54BIZx5FF50ml0iLtYGXXqW8D4MxSs2L9zWy1Ydow9ep85DB+vNLp7ujd8Ta4QhmnS6y1S8
dJUoe1ZRETEzjMRsApHP+c5UAcpIGb84YlwIDAQAB to MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6aODKvT7Alv5DhktdrgWIfsAQrwrTEPMkvggP7aYPiP
2p0DSC06I0tn6YHtr0xbB/pPXoSJv785wID5e/7mU96R69UUnbLvc5fRWVcdMP23jrECqJxSmVMYfmD9ag2E9cVkbgwTnWwIpSKg0Ie1hqroDqMwqBFlKjWizgcp2qzTp4xZ
hfOoxd98OacuRDEArgaH6uNUKDIc8Ef5JWzB/o443ZzF5TCl2mWejHiRQwbtm54BIZx5FF50ml0iLtYGXXqW8D4MxSs2L9zWy1Ydow9ep85DB+vNLp7ujd8Ta4QhmnS6y1S8
dJUoe1ZRETEzjMRsApHP+c5UAcpIGb84YlwIDAQAB

--------------------------------------------------------------------------------
> exit
--------------------------------------------------------------------------------
```

## Built With

* [Maven](https://maven.apache.org/) - Build Tool and Dependency Management
* [gRPC](https://grpc.io/) - RPC framework