
# income-tax-state-benefits

This is where we make API calls from users viewing and making changes to the State Benefits section of their income tax return.

## Running the service locally

You will need to have the following:
- Installed [MondoDB](https://docs.mongodb.com/manual/installation/)
- Installed/configured [service manager](https://github.com/hmrc/service-manager).

The service manager profile for this service is:

    sm --start INCOME_TAX_STATE_BENEFITS

This service runs on port: `localhost:9377`

Run the following command to start the remaining services locally:

    sudo mongod (If not already running)
    sm --start INCOME_TAX_SUBMISSION_ALL -r

### Feature Switches

| Feature    | Environments Enabled In     |
|------------|-----------------------------|
| Encryption | QA, Staging, ET, Production |

### Downstream services
All State Benefits data is retrieved / updated via the downstream system.
- IF (Integration Framework)
- Income Tax Submission

### State Benefits Sources (HMRC-Held and Customer Data)
State Benefits data can come from different sources: HMRC-Held and Customer. HMRC-Held data is state benefits data that HMRC have for the user within the tax year, prior to any updates made by the user. The state benefits data displayed in-year is HMRC-Held.

Customer data is provided by the user. At the end of the tax year, users can view any existing state benefits data and make changes (create, update and delete).

Examples of the prior user data can be found here in the [income-tax-submission-stub](https://github.com/hmrc/income-tax-submission-stub/blob/main/app/models/StateBenefitsUsers.scala)

## Ninos with stub data for State Benefits

### In-Year
| Nino      | State Benefits data                 | Source   |
|-----------|-------------------------------------|----------|
| AC160000B | State Benefits user with Claim data | Customer |

### End of Year
| Nino      | State Benefits data                 | Source   |
|-----------|-------------------------------------|----------|
| AC160000B | State Benefits user with Claim data | Customer |


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").