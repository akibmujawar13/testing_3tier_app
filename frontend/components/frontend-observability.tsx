'use client';

import { useEffect } from 'react';
import {
  getWebInstrumentations,
  initializeFaro,
} from '@grafana/faro-web-sdk';
import { TracingInstrumentation } from '@grafana/faro-web-tracing';

let faroInitialized = false;

export default function FrontendObservability() {
  useEffect(() => {
    if (faroInitialized) {
      return;
    }

    initializeFaro({
      url: 'https://faro-collector-prod-ap-south-1.grafana.net/collect/a7f12f03867bcc91b52d24f5a991db2f',
      app: {
        name: '3tier_Test_Application',
        version: '1.0.0',
        environment: 'production',
      },
      instrumentations: [
        ...getWebInstrumentations(),
        new TracingInstrumentation(),
      ],
    });

    faroInitialized = true;
  }, []);

  return null;
}
