# Foreign Exchange

In this app, you can trigger the `CreateFxTransaction` client-sided workflow to request the creation of a Foreign Exchange transaction from one currency to another with any other member vNode within the network

### Constraints:
1. The supported currencies are: `[ GBP, EUR, USD, CAD ]`
2. The requested transaction amount must be greater than 0.00 of any currency
3. The conversion rates are hardcoded. Currently, it is difficult to create HTTP requests inside the sandbox environment.

(*Note: It is not impossible, but it is a separate advanced topic that merits its own sample or be an extended sample of this
Stay tuned with update notes to see when easy external messaging within the sandbox environment will be released!*)

