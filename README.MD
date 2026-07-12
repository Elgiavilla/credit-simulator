# Credit Simulator

A console application that calculates the monthly installments of a vehicle loan
(car or motorcycle, new or used) over a tenure of one to six years.

| | |
|---|---|
| **Language** | Java 11 |
| **Build** | Maven |
| **Test framework** | JUnit 5 (test scope only; it never ships in the jar) |

---

## Quick start

```bash
git clone <this-repo>
cd credit-simulator

mvn clean package        # build and run the tests
./credit_simulator       # go
```

The launcher builds the jar for you if it is missing, so `./credit_simulator` on a
fresh clone also works on its own.

---

## How to run

### Interactive

```bash
./credit_simulator
# or, equivalently:
bin/credit_simulator
```

You will get a prompt. Type `show` to see every command.

```
=====================================
       CREDIT SIMULATOR
=====================================
Type 'show' to list the available commands.

> show
Available commands:
  calculate  Simulate a new vehicle loan and show the monthly installments.
  load       Load an existing calculation from the web service and show its installments.
  save       Save the current calculation as a named sheet.
  switch     Switch to a saved sheet and show its calculation again.
  sheets     List the saved sheets.
  show       List every available command.
  exit       Leave the credit simulator.
```

A full session:

```
> calculate
Jenis Kendaraan (Motor|Mobil) : Mobil
Kondisi (Baru|Bekas)         : Bekas
Tahun Kendaraan (4 digit)    : 2020
Jumlah Pinjaman Total        : 100.000.000
Tenor Pinjaman (1-6 tahun)   : 3
Jumlah DP                    : 25000000

Jenis Kendaraan   : Mobil
Kondisi           : Bekas
Tahun Kendaraan   : 2020
Jumlah Pinjaman   : Rp. 100,000,000.00
Jumlah DP         : Rp. 25,000,000.00
Pokok Pinjaman    : Rp. 75,000,000.00
Tenor             : 3 tahun

tahun 1 : Rp. 2,250,000.00/bln , Suku Bunga : 8%
tahun 2 : Rp. 2,432,250.00/bln , Suku Bunga : 8,1%
tahun 3 : Rp. 2,641,423.50/bln , Suku Bunga : 8,6%

> save
Sheet name : Skenario A
Saved as sheet 'Skenario A'.

> exit
Terima kasih. Sampai jumpa!
```

Amounts are accepted however you actually type them: `100000000`, `100.000.000`,
`100,000,000` and `Rp 100000000` all mean the same thing. A mistyped field is asked
again; the fields you already got right are not.

### With a file

```bash
./credit_simulator file_inputs.txt
# or:
bin/credit_simulator file_inputs.txt
```

The file may be **either** of two things, and the application works out which:

**A loan form** — just the six answers, in the order the application asks for them.
The calculation is implied.

```
# file_inputs.txt
Mobil           # Jenis Kendaraan  (Motor | Mobil)
Bekas           # Kondisi          (Baru | Bekas)
2020            # Tahun Kendaraan  (4 digit)
100000000       # Jumlah Pinjaman Total
3               # Tenor Pinjaman   (1-6 tahun)
25000000        # Jumlah DP

exit
```

**A script** — the exact lines you would have typed, commands included.

```
calculate
Mobil
Bekas
2020
100000000
3
25000000
save
Skenario A
show
exit
```

Blank lines and `#` comments are ignored in both.

> **One asymmetry worth knowing.** A file passed as an *argument* is read by the script
> loader, which is what infers the missing `calculate`. Redirected stdin
> (`./credit_simulator < file.txt`) is raw input to the command loop and never reaches
> that loader, so a bare loan form will not work that way — include an explicit
> `calculate` line if you redirect. The argument form is the one the spec documents and
> the one to prefer.

### With Docker

```bash
docker run -it --rm elgi/credit-simulator                    # interactive
docker run --rm elgi/credit-simulator file_inputs.txt        # the bundled sample
```

Or build it yourself:

```bash
docker build -t credit-simulator .
docker run -it --rm credit-simulator
```

### Loading an existing calculation from the web service

```
> load
```

**The URL printed in the problem statement is malformed.**
`https://run.mocky.io/v3/9108b1da-beec-409e-ae14-e212003666c` ends in an
**eleven**-character segment; a UUID's last group is twelve, and the whole id is 35
characters where a UUID is 36. One character was lost, almost certainly when the URL
was copied into the PDF. As printed, it will 404.

So the endpoint is not hard-coded. Point it wherever it really lives, without rebuilding:

```bash
java -Dcreditsimulator.api.url=https://run.mocky.io/v3/<the-real-id> \
     -jar target/credit-simulator.jar
```

---

## How to run the tests

```bash
mvn test                                     # everything
mvn test -Dtest=InstallmentCalculatorTest    # one class
mvn test -Dtest=InstallmentCalculatorTest#yearOne   # one test
```

Reports land in `target/surefire-reports/`.

The suite includes a **golden test** pinning the engine to the worked example in the
reference spreadsheet (`Rumus.xlsx`), and an end-to-end test that drives the whole
console — keystrokes in, transcript out — and asserts the same figures come out of the
screen. If a change breaks the arithmetic, both go red.

---

## Project structure

```
credit-simulator/
├── credit_simulator              # launcher (spec: an executable at the project root)
├── bin/credit_simulator          # the spec shows `bin/credit_simulator` too; a forwarder, not a copy
├── file_inputs.txt               # sample input
├── pom.xml
├── Dockerfile                    # multi-stage: JDK builds, JRE ships, non-root
├── .dockerignore
├── .github/workflows/ci.yml      # build → test → smoke-test the jar → publish to Docker Hub
│
├── src/main/java/com/elgi/creditsimulator/
│   ├── Main.java                 # entry point: picks the streams and the clock, nothing else
│   │
│   ├── model/                    # the domain. No I/O, no rules, no formatting.
│   │   ├── VehicleType.java          # Mobil | Motor  (a pure input token)
│   │   ├── VehicleCondition.java     # Baru | Bekas   (owns the 35% / 25% DP minimum)
│   │   ├── Vehicle.java              # abstract: the polymorphic seam
│   │   ├── Car.java                  #   base rate 8%
│   │   ├── Motorcycle.java           #   base rate 9%
│   │   ├── LoanRequest.java          # the inputs
│   │   ├── YearlyInstallment.java    # one row of the schedule
│   │   └── InstallmentPlan.java      # the whole schedule
│   │
│   ├── factory/
│   │   └── VehicleFactory.java    # the only class that knows which Vehicle subclasses exist
│   │
│   ├── service/                   # the arithmetic
│   │   ├── InterestRateCalculator.java   # the year-on-year rate progression
│   │   └── InstallmentCalculator.java    # the declining-principal schedule
│   │
│   ├── validation/                # one class per rule in the spec, so the two read alike
│   │   ├── ValidationRule.java
│   │   ├── VehicleYearRule.java        # rule 1: a new vehicle may not predate currentYear - 1
│   │   ├── TenureRule.java             # rule 2: 1 to 6 years
│   │   ├── LoanAmountRule.java         #         <= 1 miliar, inclusive
│   │   ├── DownPaymentRule.java        # rules 3 & 4: >= 35% new, >= 25% used
│   │   └── LoanValidator.java          # runs them all and reports EVERY failure at once
│   │
│   ├── controller/                # the C in MVC
│   │   ├── SimulatorController.java  # the read-eval-print loop, and the whole object graph
│   │   ├── Command.java              # every command carries its own name and description...
│   │   ├── CommandRegistry.java      # ...so the `show` menu is generated, never hand-maintained
│   │   ├── CommandOutcome.java
│   │   ├── LoanRequestReader.java    # prompts for the six fields, parses them
│   │   └── command/
│   │       ├── SimulateCommand.java  #   calculate
│   │       ├── LoadCommand.java      #   load
│   │       ├── SaveCommand.java      #   save
│   │       ├── SwitchCommand.java    #   switch
│   │       ├── SheetsCommand.java    #   sheets
│   │       ├── ShowCommand.java      #   show
│   │       └── ExitCommand.java      #   exit
│   │
│   ├── view/                      # the V in MVC
│   │   ├── ConsoleView.java              # streams are injected -- which is what makes it testable
│   │   └── InstallmentPlanFormatter.java # what the output says (the view owns only where it goes)
│   │
│   ├── session/
│   │   └── SheetManager.java       # named calculations; `save` / `switch` / `sheets`
│   │
│   ├── json/                       # hand-written: the spec forbids external libraries
│   │   ├── JsonParser.java             # recursive descent; numbers become BigDecimal, never double
│   │   └── JsonObject.java             # typed access with error messages worth reading
│   │
│   ├── remote/
│   │   ├── HttpFetcher.java            # the one interface that knows the network exists
│   │   ├── JdkHttpFetcher.java         # java.net.http.HttpClient -- ships with the JDK
│   │   └── LoanApiClient.java          # payload -> LoanRequest
│   │
│   ├── io/
│   │   └── InputScript.java        # reads file_inputs.txt; tells a script from a bare form
│   │
│   ├── utils/
│   │   └── MoneyFormat.java        # shared by view AND validation, so neither depends on the other
│   │
│   └── exception/
│       ├── CreditSimulatorException.java   # base: caught at the edges, never shown as a stack trace
│       ├── InvalidInputException.java      #   "we cannot understand that"
│       ├── ValidationException.java        #   "we understand it, but the rules forbid it"
│       ├── InputFileException.java         #   "the file itself is the problem"
│       └── RemoteServiceException.java     #   "the service let us down, not you"
│
└── src/test/
    ├── factory/VehicleFactoryTest.java
    ├── service/InterestRateCalculatorTest.java
    ├── service/InstallmentCalculatorTest.java     # <- the golden test against Rumus.xlsx
    ├── validation/LoanValidatorTest.java
    ├── controller/SimulatorControllerTest.java    # <- drives the whole console, end to end
    ├── utils/MoneyFormatTest.java
    ├── json/JsonParserTest.java
    ├── io/InputScriptTest.java
    └── session/SheetManagerTest.java
```

---

## The formula

The reference spreadsheet (`Rumus.xlsx`) describes a **declining-principal, yearly
re-quoted flat rate** scheme — not a standard annuity. For year `n` of a loan of `T` years:

```
openingPrincipal(1) = loanAmount - downPayment
openingPrincipal(n) = totalDue(n-1) - yearlyInstallment(n-1)

totalDue(n)           = openingPrincipal(n) * (1 + rate(n))
monthlyInstallment(n) = totalDue(n) / (12*T - 12*(n-1))
yearlyInstallment(n)  = monthlyInstallment(n) * 12
```

The divisor shrinks by twelve each year, which is why the monthly payment **rises** over
the life of the loan even as the principal falls. Commercially unusual, but it is what
the spreadsheet does, and the spreadsheet is the source of truth.

### The interest rate progression

Base rate: **Car 8%**, **Motorcycle 9%**.

The spec gives two rules that contradict each other if you stack them:

* *"Interest Year On Year increase 0,5% every 2 year."*
* *"Interest Year On Year increase 0,1% every 1 year."*

Applied cumulatively, year 3 of a car loan would be `8 + 0.1 + 0.1 + 0.5 = 8.7%`. But
the worked example in the same document says year 3 is **8.6%**. The example is the
tiebreaker: the increments do not stack, they **alternate** across year boundaries.

| stepping from year `k` to `k+1` | increment |
|---|---|
| k odd (1→2, 3→4, 5→6) | **+0.1%** |
| k even (2→3, 4→5) | **+0.5%** |

* Car: `8.0 → 8.1 → 8.6 → 8.7 → 9.2 → 9.3`
* Motorcycle: `9.0 → 9.1 → 9.6 → 9.7 → 10.2 → 10.3`

### The golden case

The spreadsheet's own worked example (price 100,000,000 / DP 25% / tenure 3 years) is
pinned by a test, and asserted again end-to-end through the console:

| Year | Opening principal | Rate | Total due | Monthly |
|---|---|---|---|---|
| 1 | 75,000,000 | 8.0% | 81,000,000 | **2,250,000.00** |
| 2 | 54,000,000 | 8.1% | 58,374,000 | **2,432,250.00** |
| 3 | 29,187,000 | 8.6% | 31,697,082 | **2,641,423.50** |

---

## Lending rules

| # | Rule | Enforced by |
|---|---|---|
| 1 | A vehicle in **new** condition may not have a model year earlier than `currentYear - 1` | `VehicleYearRule` |
| 2 | Tenure is 1 to 6 years | `TenureRule` |
| 3 | Down payment for a **new** vehicle ≥ **35%** of the loan amount | `DownPaymentRule` |
| 4 | Down payment for a **used** vehicle ≥ **25%** of the loan amount | `DownPaymentRule` |
| — | Loan amount ≤ **1,000,000,000** (inclusive) | `LoanAmountRule` |

All bounds are inclusive: exactly 35% passes, exactly one billion passes.

Every broken rule is reported at once, not one at a time:

```
> calculate
...
Cannot proceed. 2 problems with this request:
  - Loan tenure must be between 1 and 6 years, but was 7.
  - Down payment for a Mobil Baru must be at least 35% of the loan amount
    (Rp. 35,000,000.00), but was Rp. 10,000,000.00 (10%).
```

---

## Design notes

**No external libraries.** The spec forbids them, so there are none — not even for JSON
or HTTP. `java.net.http.HttpClient` ships with Java 11, and the JSON parser is
hand-written recursive descent. JUnit is `test` scope and never reaches the jar.

**Money is `BigDecimal` everywhere, never `double`.** The payload carries a loan of one
billion rupiah; a `double` has 53 bits of mantissa and cannot represent every value in
that range exactly. That includes the JSON parser, which turns every number into a
`BigDecimal` at the gate.

**The calculation chain does not round.** Rounding the monthly figure inside the loop
would feed a rounded number into the yearly figure, and from there into the next year's
opening principal — so the error would compound across the tenure and drift away from
the spreadsheet. High precision throughout; rounded only for display.

**The `show` menu is generated from the command registry.** A hard-coded menu is a second
copy of the truth, and second copies rot. Registering a new command is one line, and the
menu cannot be wrong.

**`ConsoleView` takes its streams as constructor arguments.** That single decision is what
makes the whole console testable: a test hands it a string of keystrokes and reads back
the transcript. No mocks, no captured stdout.

**`VehicleYearRule` takes an injected `Clock`.** "The current year" is the one input that
changes without anyone editing it. Read from `Year.now()`, a test asserting that a 2025
model is valid would be green this year and red the next. The spec's own sample payload is
a 2025 vehicle — which stops being a valid *new* vehicle in 2027.

### Two defects in the source material, handled deliberately

1. **The sample output in the problem statement** (`3,500,000 → 3,501,000 → 3,505,000`)
   does not follow the spreadsheet formula. It is treated as an illustration of the output
   *format* only, never as a test fixture.

2. **Cell `D9` of the spreadsheet** ("Monthly Rata2") divides by a hard-coded `60` instead
   of `12 × tenure` — correct only for a five-year loan. We divide by the real number of
   months. A test asserts both figures, so the divergence is on the record as a decision
   rather than an accident.

The number formats in the sample are also internally inconsistent — money uses US
separators (`Rp. 3,500,000.00`) while the rate uses an Indonesian decimal comma (`8,1%`),
on the same line. That inconsistency is reproduced faithfully, because matching the spec's
output beats being right about typography.

---

## CI/CD

`.github/workflows/ci.yml` runs on every push and pull request:

1. **Build** on JDK 11 (Temurin), with the Maven cache.
2. **Test** — the full JUnit suite; reports are uploaded even when the build fails, which
   is when they are most useful.
3. **Package**, then **smoke-test the jar itself** — the sample input is fed through the
   real artefact and the golden figures are grepped out of its output. A green build that
   produces a jar nobody executed is a green build that means very little.
4. **Publish to Docker Hub** — only on `main` or a `v*` tag, and only if the tests passed.
   A pull request from a fork can never push an image.

Two repository secrets are required for the publish step:

| Secret | Value |
|---|---|
| `DOCKERHUB_USERNAME` | your Docker Hub username |
| `DOCKERHUB_TOKEN` | a Docker Hub **access token** (never the account password) |