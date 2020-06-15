import { NativeModules } from 'react-native';

const { Nearby } = NativeModules;

export type Nearby = {
  startAdvertising: (strategyID: number, nickname: string) => Promise<void>;
  startDiscovery: (strategyID: number, nickname: string) => Promise<void>;
  requestConnection: (endpointID: string, nickname: string) => Promise<void>;
  acceptConnection: (endpointID: string) => Promise<void>;
  rejectConnection: (endpointID: string) => Promise<void>;
  sendBytesPayload: (endpointID: string, base64Bytes: string) => Promise<void>;
};

export type Events = {
  PayloadReceived: {
    endpointID: string;
    data: string;
  };
  PayloadTranserUpdate: {
    endpointID: string;
    status: number;
  };
  EndpointFound: {
    endpointID: string;
    endpointName: string;
  };
  EndpointLost: {
    endpointID: string;
  };
  NearbyConnectionInitiated: {
    endpointID: string;
    endpointName: string;
    authenticationToken: string;
  };
  NearbyConnectionResult: {
    endpointID: string;
    status: number;
  };
  NearbyConnectionDisconnected: {
    endpointID: string;
  };
};

export default Nearby as Nearby;
