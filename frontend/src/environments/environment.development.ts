import { apiUrlConfig } from './environment.urls';

// Development environment configuration (Local)
export const environment = {
  production: false,
  apiUrl: apiUrlConfig.development,
  appName: 'MeghaConnect [DEV]',
  version: '1.0.0-dev'
};
