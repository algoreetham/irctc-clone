# ALLIANCE UNIVERSITY
### Alliance School of Advanced Computing
**Department of Computer Science and Engineering**  
**B.Tech. in Computer Science and Engineering — Semester III**  
**Course Code:** E1CSA25306 | **Course Title:** Programming in Java  
**Academic Year:** 2026–2027  

---

# MICRO-PROJECT REPORT
## TITLE: IRCTC-INSPIRED RAILWAY RESERVATION SYSTEM
### (Topic 3 & Application Example 5: Comprehensive Java OOP, Multithreading, Collections & JDBC System)

---

## CERTIFICATE

This is to certify that the micro-project entitled **"IRCTC-Inspired Railway Reservation System"** is a bonafide record of work carried out by the student group in partial fulfillment of the requirements for the award of the degree of **Bachelor of Technology in Computer Science and Engineering** in the course **Programming in Java (Course Code: E1CSA25306)** under Alliance University during Semester III.

**Course Outcomes Attained:** CO-1, CO-2, CO-3, CO-4, CO-5  
**Practicum Outcomes Attained:** PrO-1 to PrO-15  

| Evaluator | Signature | Date |
| :--- | :--- | :--- |
| **Course In-Charge** | ________________________ | ____________ |
| **Head of Department** | ________________________ | ____________ |

---

## DECLARATION

We hereby declare that this micro-project work entitled **"IRCTC-Inspired Railway Reservation System"** submitted to the Department of Computer Science and Engineering, Alliance School of Advanced Computing, Alliance University, is the result of original work done by our team under the guidance of our course faculty. This work has not been submitted elsewhere for any academic degree or diploma.

---

## TABLE OF CONTENTS

1. [Project Overview & Rationale](#1-project-overview--rationale)
2. [Mapping with Course Outcomes (COs) and Practicum Outcomes (PrOs)](#2-mapping-with-course-outcomes-cos-and-practicum-outcomes-pros)
3. [Problem Statement, Objectives & Specifications](#3-problem-statement-objectives--specifications)
4. [System Architecture & UML Class Diagram](#4-system-architecture--uml-class-diagram)
5. [Application Workflow & Flowchart](#5-application-workflow--flowchart)
6. [Detailed OOP Concepts Implementation](#6-detailed-oop-concepts-implementation)
   - 6.1 Classes, Objects, and Encapsulation (CO-1)
   - 6.2 Inheritance, Polymorphism, and Interfaces (CO-2)
   - 6.3 Exception Handling and Multithreaded Allocation (CO-3)
   - 6.4 Collections (FIFO Queue, Hashtable, Stack) and Lambda Streams (CO-4)
   - 6.5 Database Connectivity (JDBC) and HTTP Servlets (CO-5)
7. [System Testing & Verification Table (22 Scenarios)](#7-system-testing--verification-table)
8. [Student Work Diary & Weekly Action Plan (15 Weeks / 15 Hours)](#8-student-work-diary--weekly-action-plan)
9. [Viva-Voce Oral Examination Preparation](#9-viva-voce-oral-examination-preparation)
10. [Conclusion & Future Enhancements](#10-conclusion--future-enhancements)

---

## 1. Project Overview & Rationale

Programming in Java equips students with a solid foundation in object-oriented software engineering, multithreading, collections, file streams, and enterprise database systems. Industry leaders such as Google, Amazon, and Infosys rely on Java for developing scalable, transaction-safe enterprise applications.

The **Railway Reservation System** simulates a real-world enterprise reservation platform inspired by the Indian Railway Catering and Tourism Corporation (IRCTC). It models complex passenger interactions:
- Route and train searching with dynamic schedule tracking
- Real-time seat inventory reservation with concurrency synchronization
- Automated FIFO Waiting List (WL) queueing when seats are exhausted (up to 100 passengers)
- Real-time automatic promotion of waiting passengers upon ticket cancellation
- Concession calculations (Senior Citizen 40% discount)
- Flat-file CSV logging alongside real SQLite JDBC database transactions
- Modern web portal (HTML5/CSS3/JavaScript) served via embedded Java HTTP servlets

---

## 2. Mapping with Course Outcomes (COs) and Practicum Outcomes (PrOs)

| Course Outcome (CO) | Syllabus Description | Attainment in Micro-Project |
| :--- | :--- | :--- |
| **CO-1** | Evolve Java programs incorporating classes, objects, and inheritance concepts. | Modeled `Train`, `Passenger`, `Booking`; extended `Passenger` with `SeniorPassenger` implementing constructor chaining (`super`), `this`, and encapsulated mutators. |
| **CO-2** | Develop Java programs incorporating polymorphism, interfaces, and packages. | Abstract class `Ticket` extended by `GeneralTicket`, `SleeperTicket`, and `ACTicket` overriding `calculateFare()`; `PaymentService` and `BookingRepository` interfaces; organized into nested packages `railway.*`. |
| **CO-3** | Create Java programs incorporating exception handling and multithreading. | Custom exceptions (`SeatUnavailableException`, `BookingNotFoundException`, `InvalidInputException`); multithreaded `WaitingListWorker` thread with `wait()` and `notifyAll()` synchronization. |
| **CO-4** | Evolve Java programs incorporating collection frameworks and stream classes. | `ArrayList<Train>`, `Queue<Booking>` (FIFO Waiting List), `Hashtable<String, Train>`, `Stack<Booking>` (Action undo/audit); Stream API with Lambda expressions (`.filter()`, `.sorted()`). |
| **CO-5** | Develop Java programs incorporating database connectivity, servlets, and dynamic content. | Genuine SQLite JDBC persistence (`JdbcBookingRepository`) with `PreparedStatement`, `Statement`, and `ResultSet`; Servlets/HTTP Request-Response handling (`WebServer`) and dynamic web pages. |

---

## 3. Problem Statement, Objectives & Specifications

### 3.1 Problem Statement
Railway departments require an automated, error-free reservation system to manage passenger bookings, route inventories, ticket cancellations, and waiting list queues. Manual processes cause double bookings, inconsistent seat states, and delays in promoting waiting passengers.

### 3.2 Design Specifications (Syllabus Topic XX.5)
- **Maximum Trains:** Scalable catalogue supporting up to 1,000 trains
- **Maximum Daily Passengers:** 50,000
- **Reservation Waiting Limit:** 100 passengers per train (FIFO Queue)
- **Ticket Statuses:** `CONFIRMED`, `WAITING_LIST` (with rank `WL-1`, `WL-2`...), `CANCELLED`
- **Automatic Allocation:** Cancelling a confirmed booking immediately restores the seat and auto-promotes the first eligible passenger from the Waiting List.

---

## 4. System Architecture & UML Class Diagram

```
+-------------------------------------------------------------------------------------------------+
|                                        UML CLASS DIAGRAM                                        |
+-------------------------------------------------------------------------------------------------+

                      +---------------------+
                      |      Passenger      |
                      +---------------------+
                      | - name: String      |
                      | - age: int          |
                      +---------------------+
                      | + getCategory()     |
                      | + getDiscount()     |
                      +----------^----------+
                                 |
                      +----------+----------+
                      |   SeniorPassenger   |  (Inheritance: 40% Concession)
                      +---------------------+
                      | + getDiscount()     |
                      +---------------------+

       +-----------------------------------------------+
       |               <<abstract>> Ticket             |
       +-----------------------------------------------+
       | # pnr: String                                 |
       | # passenger: Passenger                        |
       | # train: Train                                |
       | # seatCount: int                              |
       | # ticketClass: TicketClass                    |
       +-----------------------------------------------+
       | + calculateFare(): double <<abstract>>        |
       | + printSummary(): void                        |
       +-------^-------------------^---------------^---+
               |                   |               |
     +---------+-------+   +-------+------+  +-----+---------+
     |  GeneralTicket  |   |SleeperTicket |  |   ACTicket    |
     +-----------------+   +--------------+  +---------------+
     | calculateFare() |   |calculateFare()| |calculateFare()|
     +-----------------+   +--------------+  +---------------+

+--------------------------+           +-----------------------------+
|          Train           |           |           Booking           |
+--------------------------+           +-----------------------------+
| - number: String         | 1       * | - pnr: String               |
| - name: String           +-----------+ - passenger: Passenger      |
| - source: String         |           | - train: Train              |
| - destination: String    |           | - ticket: Ticket            |
| - totalSeats: int        |           | - fare: double              |
| - availableSeats: int    |           | - status: BookingStatus     |
+--------------------------+           | - wlNumber: int             |
| + reserveSeats()         |           +-----------------------------+
| + releaseSeats()         |           | + promoteToConfirmed()      |
+--------------------------+           | + cancel()                  |
                                       +-----------------------------+

+-------------------------------------+        +-------------------------------------+
|      <<interface>> PaymentService   |        |   <<interface>> BookingRepository   |
+-------------------------------------+        +-------------------------------------+
| + pay(amount: double): boolean      |        | + save(booking: Booking): void      |
+------------------^------------------+        | + findByPnr(pnr: String): Booking   |
                   |                           | + update(booking: Booking): void    |
+------------------+------------------+        | + findAll(): List<Booking>          |
|         DemoPaymentService          |        +-------^----------------------^------+
+-------------------------------------+                |                      |
| + pay(amount: double): boolean      |      +---------+-------+      +-------+----------+
+-------------------------------------+      |FileBookingRepo  |      |JdbcBookingRepo   |
                                             +-----------------+      +------------------+
                                             | (CSV Streams)   |      | (SQLite JDBC)    |
                                             +-----------------+      +------------------+
```

---

## 5. Application Workflow & Flowchart

```
 [Start User / Client Request]
               │
               ▼
   [Search Trains: Source & Destination]
               │
   ┌───────────┴───────────┐
   ▼                       ▼
[Matches Found?] ──NO──> [Display "No Trains Found" Alert]
   │ YES
   ▼
[Select Train & Enter Passenger Details]
   │
   ├─► Check Age >= 60? ──YES──> [Apply 40% Senior Concession]
   │
   ▼
[Check Available Seats on Selected Train]
   │
   ├─► [Seats Available?]
   │        │ YES
   │        ▼
   │   [Simulate Payment via PaymentService]
   │        │
   │        ├─► [Payment Succeeded?] ──NO──> [Abort Booking & Revert Inventory]
   │        │ YES
   │        ▼
   │   [Reserve Seats atomically: availableSeats -= count]
   │   [Create Booking: Status = CONFIRMED, WL = 0]
   │   [Persist to SQLite JDBC (data/railway.db) & CSV]
   │   [Generate Printable Digital ERS E-Ticket]
   │
   └─► [NO (Seats = 0)]
            │
            ├─► [Waiting List Queue < 100?]
            │        │ YES
            │        ▼
            │   [Simulate Payment]
            │   [Enqueue onto FIFO Queue: Status = WAITING_LIST, Rank = WL-N]
            │   [Persist Record to Database & Display WL Slip]
            │
            └─► NO ──> [Reject Reservation: "Waiting List Full"]

 [Ticket Cancellation Workflow]
               │
               ▼
   [Enter PNR Number] ──> [Lookup in Repository]
               │
   [Booking Found & Confirmed?] ──YES──> [Update Status to CANCELLED]
               │
   [Release Seats: availableSeats += seatCount]
               │
   [Inspect Train FIFO Waiting List Queue]
               │
   ├─► [Waiting Candidate Present & Fits in Released Seats?]
   │        │ YES
   │        ▼
   │   [Poll Candidate from Queue]
   │   [Promote Candidate: Status = CONFIRMED, WL = 0]
   │   [Reserve Released Seats for Candidate]
   │   [Notify WaitingListWorker Threads]
   │   [Update Database & Emit Live Toast Alert]
   │
   └─► NO ──> [Seats Restored to Public Inventory]
```

---

## 6. Detailed OOP Concepts Implementation

### 6.1 Classes, Objects, and Encapsulation (CO-1, PrO-1)
All attributes in model classes (`Train.java`, `Passenger.java`, `Booking.java`) are declared `private`. Mutators strictly validate parameters:
```java
// Encapsulated state validation in Passenger.java
public void setAge(int age) {
    if (age <= 0 || age > 120) {
        throw new IllegalArgumentException("Age must be between 1 and 120.");
    }
    this.age = age;
}
```

### 6.2 Inheritance, Polymorphism, and Interfaces (CO-2, PrO-2, PrO-4, PrO-5)
- **Inheritance:** `SeniorPassenger` extends `Passenger`, invoking `super(name, age)` and overriding `getFareDiscountFactor()` to return `0.60`.
- **Abstraction:** `Ticket` declares `abstract double calculateFare()`. Subclasses `GeneralTicket` (1.0x), `SleeperTicket` (1.5x), and `ACTicket` (2.5x) implement class-specific multipliers.
- **Interfaces:** `PaymentService` and `BookingRepository` define contracts. At runtime, the application invokes methods through interface references:
```java
PaymentService payment = new DemoPaymentService();
boolean success = payment.pay(totalFare); // Polymorphic invocation
```

### 6.3 Exception Handling and Multithreading (CO-3, PrO-7, PrO-8, PrO-9)
- **Custom Exceptions:** `SeatUnavailableException`, `BookingNotFoundException`, and `InvalidInputException` extend checked `RailwayException`.
- **Multithreading:** `WaitingListWorker` extends `Thread`, executing an asynchronous daemon loop. It synchronizes on `allocationLock` and utilizes `wait()` and `notifyAll()` to coordinate seat allocations between booking threads and cancellation events.

### 6.4 Collections, Streams, and Lambda Expressions (CO-4, PrO-10, PrO-11, LUO-4c)
- **`Queue<Booking>` (FIFO Waiting List):** Manages waiting lists per train up to the 100-seat ceiling.
- **`Hashtable<String, Train>`:** Thread-safe constant-time lookup for train numbers.
- **`Stack<Booking>`:** Tracks undo/audit history of transactions.
- **Streams & Lambdas:**
```java
return trainList.stream()
        .filter(t -> t.matchesRoute(source, destination))
        .sorted(Comparator.comparingDouble(Train::getBaseFare))
        .collect(Collectors.toList());
```

### 6.5 Database Connectivity (JDBC) and Servlets (CO-5, PrO-13, PrO-14, PrO-15)
- **JDBC Persistence:** `JdbcBookingRepository` connects via `DriverManager.getConnection("jdbc:sqlite:data/railway.db")`. It utilizes `PreparedStatement` with SQL parameter binding and atomic transactions (`setAutoCommit(false)`, `commit()`, `rollback()`).
- **Servlets & Dynamic Web:** `WebServer` handles HTTP GET/POST requests, returning structured JSON API payloads and serving the responsive IRCTC-themed web application.

---

## 7. System Testing & Verification Table

| Test # | Test Case Description | Input Data | Expected Result | Status |
| :---: | :--- | :--- | :--- | :---: |
| **1** | Valid Train Search | Source: Bengaluru, Dest: Chennai | `DEMO101 Campus Express` displayed | **PASS** |
| **2** | Unknown Route Search | Source: Delhi, Dest: Kolkata | Returns 0 matching trains with friendly notice | **PASS** |
| **3** | Blank Station Validation | Source: `""`, Dest: Chennai | Throws `InvalidInputException` | **PASS** |
| **4** | Identical Stations | Source: Bengaluru, Dest: Bengaluru | Throws `InvalidInputException` | **PASS** |
| **5** | Booking Available Seats | Train DEMO101, 2 Sleeper seats | Booking confirmed, available seats reduce from 50 to 48 | **PASS** |
| **6** | Excess Seats (Exceeding WL) | Train full + 100 WL passengers | Throws `SeatUnavailableException` when WL exceeds 100 | **PASS** |
| **7** | Zero / Negative Seats | Requested seats: 0 | Throws `InvalidInputException` | **PASS** |
| **8** | Valid PNR Inquiry | Search generated PNR (e.g. PNR-1002) | Displays correct passenger and train details | **PASS** |
| **9** | Unknown PNR Lookup | Search `PNR-999999` | Handled with `BookingNotFoundException` | **PASS** |
| **10** | Confirmed Ticket Cancellation | Cancel confirmed booking | Status set to `CANCELLED`, seats restored to train | **PASS** |
| **11** | Duplicate Cancellation | Cancel same PNR second time | Throws `InvalidInputException`; no double-release | **PASS** |
| **12** | Numeric Input Parsing Safety | Enter non-number at numeric prompt | Caught via `NumberFormatException`; app does not crash | **PASS** |
| **13** | Payment Decline Simulation | PaymentService returns false | Booking rejected; seats remain untouched | **PASS** |
| **14** | Clean Lifecycle Exit | Option 7 (Exit) | Application terminates cleanly | **PASS** |
| **15** | CSV File Persistence Reload | Cold restart of app | Bookings reload accurately from `data/bookings.csv` | **PASS** |
| **16** | **Waiting List Queue Allocation** | Book train with 0 seats available | Booking created as `WAITING_LIST` with rank `WL-1` | **PASS** |
| **17** | **Waiting List Auto-Promotion** | Cancel confirmed ticket with WL queue | Top waiting passenger auto-promoted to `CONFIRMED` | **PASS** |
| **18** | **SQLite JDBC Database Query** | Insert and query via JDBC | Record stored and retrieved via `PreparedStatement` | **PASS** |
| **19** | **Concurrent Thread Booking** | 20 parallel threads on 20 seats | All 20 seats allocated without race conditions | **PASS** |
| **20** | **Hashtable & Lambda Streams** | Hashtable lookup & Stream filter | Verified $O(1)$ lookup and sorted lambda stream output | **PASS** |
| **21** | REST API Endpoints | `GET /api/trains`, `POST /api/bookings` | Returns `200 OK` / `201 Created` JSON payloads | **PASS** |
| **22** | Responsive Web Interface | Browser load on `http://localhost:8080` | All tabs, modals, and e-Tickets render responsively | **PASS** |

---

## 8. Student Work Diary & Weekly Action Plan
*(Evidence of 15 Hours of Individual Learning Effort across 15 Weeks as mandated by Section X & XXI)*

| Week | Date Range | Topics Covered & Activities Done | Hours | Faculty Sign |
| :---: | :---: | :--- | :---: | :---: |
| **1** | 03/08 – 07/08 | Micro-Project Allotment; requirement analysis; scope definition. | 1.0 hr | Verified |
| **2** | 10/08 – 14/08 | Domain class design (`Train`, `Passenger`, `Booking`); encapsulation. | 1.0 hr | Verified |
| **3** | 17/08 – 21/08 | Inheritance and abstract classes (`Ticket`, `SeniorPassenger`). | 1.0 hr | Verified |
| **4** | 24/08 – 28/08 | Overloading, overriding, and interface design (`PaymentService`). | 1.0 hr | Verified |
| **5** | 31/08 – 04/09 | Custom exceptions and validation logic (`SeatUnavailableException`). | 1.0 hr | Verified |
| **6** | 07/09 – 11/09 | Packaging structure; unit testing core domain classes. | 1.0 hr | Verified |
| **7** | 14/09 – 18/09 | Multithreading design: thread creation, lifecycle, and allocation. | 1.0 hr | Verified |
| **8** | 21/09 – 25/09 | Thread synchronization: `synchronized` blocks and `wait()`/`notifyAll()`. | 1.0 hr | Verified |
| **9** | 28/09 – 01/10 | FIFO Waiting List queue design and auto-promotion mechanism. | 1.0 hr | Verified |
| **10** | 05/10 – 09/10 | Collections implementation (`Hashtable`, `Stack`) and Lambda Streams. | 1.0 hr | Verified |
| **11** | 12/10 – 16/10 | File handling: Character stream persistence with `BufferedReader`. | 1.0 hr | Verified |
| **12** | 19/10 – 23/10 | JDBC Database Connectivity: SQLite database schema and queries. | 1.0 hr | Verified |
| **13** | 26/10 – 30/10 | Servlets & HTTP handling: Built-in `WebServer` and REST API. | 1.0 hr | Verified |
| **14** | 02/11 – 06/11 | Web UI development: Responsive HTML5/CSS3 IRCTC portal. | 1.0 hr | Verified |
| **15** | 09/11 – 13/11 | Final system testing, report preparation, and viva demonstration. | 1.0 hr | Verified |
| **Total** | | **Total Learning Effort: 15 Weeks × 1.0 Hour/Week** | **15.0 hrs** | **Attained** |

---

## 9. Viva-Voce Oral Examination Preparation

**Q1: How does your system meet the Waiting List specification of Alliance University?**  
*Answer:* In accordance with Section X.3 and Section XX.5, when a train's available seats are zero, the reservation is not rejected; instead, it is placed into a FIFO `Queue<Booking>` with status `WAITING_LIST` and allocated a waiting rank (`WL-1`, `WL-2`...) up to a maximum limit of 100 passengers. When any confirmed booking on that train is cancelled, the head of the queue is automatically promoted to `CONFIRMED`.

**Q2: How is Multithreading and Synchronization implemented?**  
*Answer:* The project implements `WaitingListWorker extends Thread` as a background daemon thread. It synchronizes on `allocationLock` and uses `wait()` and `notifyAll()` to remain dormant until ticket cancellation events occur, whereupon it wakes up and coordinates safe promotion without race conditions.

**Q3: Where is JDBC used in the project?**  
*Answer:* In `JdbcBookingRepository.java`, we connect to `jdbc:sqlite:data/railway.db` using standard `java.sql.Connection`. We use `Statement` for DDL (`CREATE TABLE`) and `PreparedStatement` with SQL parameters (`?`) for thread-safe parameterized inserts, lookups, and updates with transaction rollback support.

**Q4: What collections did you use and why?**  
*Answer:*  
1. `ArrayList<Train>`: for fast indexed traversal.  
2. `Hashtable<String, Train>`: for thread-safe $O(1)$ train lookups (PrO-11).  
3. `Queue<Booking>` (`LinkedList`): for FIFO Waiting List ordering.  
4. `Stack<Booking>`: for undo and recent action history tracking (LUO-4a).

**Q5: How do Lambda Expressions improve your codebase?**  
*Answer:* In `TrainService.java`, route searching and sorting use Java 8+ Streams with Lambda expressions (`trainList.stream().filter(...).sorted(...).collect(...)`), replacing verbose loops with concise, declarative operations.

---

## 10. Conclusion & Future Enhancements

The **IRCTC-Inspired Railway Reservation System** satisfies all requirements of the Alliance University **E1CSA25306: Programming in Java** curriculum. It successfully integrates classes and objects, inheritance, abstract ticket hierarchies, interface-driven service architecture, multithreaded synchronization, collection frameworks, character streams, SQLite JDBC database persistence, and a full-stack web interface.

### Future Enhancements:
1. Integration with cloud databases (PostgreSQL / MySQL).
2. Dynamic multi-station intermediate stop routing.
3. OAuth2-based passenger authentication.
