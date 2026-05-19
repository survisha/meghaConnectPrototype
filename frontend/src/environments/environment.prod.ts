import { apiUrlConfig } from './environment.urls';

// Pilot/Production environment configuration.
export const environment = {
  production: true,
  apiUrl: apiUrlConfig.production,
  appName: 'MeghaConnect',
  version: '1.0.0-prod'
};
