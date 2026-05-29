# SMS Templates

SMS sending is centralized under `com.survisha.common.sms`.

To add a new SMS notification:

1. Add the approved DLT template ID to `SmsTemplateConstants`.
2. Add a descriptive method to `SmsService`.
3. Implement the message text in `SmsServiceImpl`.
4. Call the `SmsService` method from the business service.

Do not build Ping4SMS URLs or hardcode template IDs in controllers.

Configuration is controlled by:

```yaml
sms:
  enabled: ${SMS_ENABLED:false}
  provider: ping4sms
  base-url: ${SMS_BASE_URL:https://site.ping4sms.com/api/smsapi}
  api-key: ${SMS_API_KEY:}
  route: ${SMS_ROUTE:2}
  sender-id: ${SMS_SENDER_ID:DEUPL}
```

Keep `SMS_ENABLED=false` for local, SIT, and UAT unless SMS delivery is explicitly being tested.
