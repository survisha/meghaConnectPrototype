// UAT/dev-server environment configuration.
// Used by the Angular "uat" build configuration in angular.json.
import { apiUrlConfig } from './environment.urls';
export const environment = {
  production: true,
  apiUrl: apiUrlConfig.dev,
  appName: 'MeghaConnect [UAT]',
  version: '1.0.0-uat'
};
