# Antaeus - My solution
My first thought process after reading the proposed challenge was to create a CRON Job that would take all pending
invoices and charge them all. But once I started browsing the code and reading all the possible event exceptions I realized
it was a harder task than I thought.

The document first gives an overview of my solution, and then it breaks down the different parts to give more detail.
##1 - Overview

---

The solution as it stands has a timer that calculates the time until the first day of the next month and triggers a function
that processes all pending invoices. If an exception is thrown during in function the error is caught and a note is added to
the invoice in order to notify the customer and admins. In the case of a Network error a timer is set that will trigger 
the billing process after an hour with a maximum number of retries.



The billing process can be triggered manually by the API as well.

---

##2 - Modifications
In order to develop my solution certain parts of the solution's skeleton had to be changed in order to accommodate my implementation:

###2.1 - Data Model
The main modification in the data model is the creation of an Invoice Note(`InvoiceNote.kt`). This enum was implements in order
to notify the customer and admins of the occurred error by modifying the invoice in the database. It also has the goal of 
distinguish new pending invoices from those that were already processed.
It supports the main exceptions and if an unrecognized exception is thrown, a generic note is appended. 
The invoice now has a `val note: String` in its data class and all the necessary modifications in `mapings.kt` and in 
`tables.kt` were implemented.

###2.2 - Data Access Layer (DAL)

In `Antaeus.kt` the functions `paidInvoice(id:Int)` and `failedPayment(id:Int,newNote:InvoiceNote)` were implemented with
the goal of modifying the database with the correct information in the case of a success or a failure in the billing process.

The `paidInvoice` modifies the invoice by changing its status.

The `failedPayment` modifies the invoice by appending a note with the error.

Additionally, the function`fetchInvoicesByStatus(status: InvoiceStatus)` was implemented in order to access the necessary invoices more easily.

###2.3 - Invoice Service
The invoice service was modified to call the new functions in the DAL.

Three functions were implemented: `failedPaymentInvoice()`, `paidInvoice()` and `fetchByStatus()` 
that call the correspondent function in the DAL.


###2.4 - Billing Service

This was where most of my time was spent. As mentioned, my first thought process was to create a CRON job but, once I realized
that it was not so simple to do without adding dependencies (by browsing the depths of the internet), I left that challenge for last.

The class receives the two already built services plus the payment provider.

I focused first on building the core logic to handle the billing of the invoices which is handled in the `executePayment(invoice:Invoice)`.
I first started building the solution to handle just the cases where the payment when smoothly, and it gradually started to become more complex 
to handle the exception thrown. Depending on the exception or failure by the payment provider, a different note is created to be added to the bill in the database. 

In a more realistic scenario, it would also trigger the sending of an email
to the customer when the payment provider could not charge due to insufficient funds or in the case of different currencies between the invoice
and account.

Then, I implemented two functions: `processPendingInvoice(id:Int)` that processes a single invoice and 
`processAllPendingInvoices()` that batch processes all pending invoices. These two functions share the same 
underlying logic by using the `executePayment(invoice:Invoice)` function.

After verifying that these functions worked as intended, I faced my fears, 
and I started to search in the documentation for a function that would help me in building the monthly billing trigger. 
I stumbled upon the `kotlin.concurrent.schedule` which allowed me to exactly what I wanted... Except modifying
a timer's period between triggers. After searching and thinking for I while, I realized that I could simply 
create a schedule, and after it triggers, delete it and create a new one with a different time delay. 
That was exactly what I did in the `billProcessingTrigger()` function which calls the batch processing function, 
deletes and creates a new schedule. The logic to calculate the time until the next month is in the `getTimeTilNextMinute()`.

When I verified that this scheme produced the desired effects (by trying with a delay of 15 seconds instead of several days), 
I realized that I could do a similar thing for the network error in the billing process. 
So, I wrote additional logic to retry the process after an hour for a given invoice if the error thrown is the network error. 
It retries for a maximum of 3 times.


###2.5 - REST API
In the REST API, I have added a query parameter to the endpoint that gets all invoices in order to filter them by status.
I've also added endpoints to trigger the billing functions manually if necessary. If a user tries to process
an invoice that was already paid it returns a 409 HTTP error.

###2.6 - Other
* In the `AntaeusApp.kt` I've added the first call of the invoice processing timer and added the billing service to the REST service.
* Created unit tests for the Billing Service.
* Changed the Docker port mapping to 7070 (port conflict related).
* Create the necessary exceptions to handle all errors.
* Had fun and learned more about Kotlin


---
## 3 - Next Improvements
* Parallel processing of all pending invoices.
* Distribute the monolithic architecture by the different services to scale the services appropriately and prevent total loss of service.
* Separate the database from the application.
---
## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus .
docker run -p 7070:7000 --name antaeus antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
