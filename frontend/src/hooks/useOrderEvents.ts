import { useCallback, useEffect, useRef, useState } from 'react';
import { OrderEvent } from '../types/order';

export function useOrderEvents() {
  const [eventsMap, setEventsMap] = useState<Record<string, OrderEvent[]>>({});
  const sourcesRef = useRef<Record<string, EventSource>>({});

  const subscribe = useCallback((orderId: string) => {
    if (sourcesRef.current[orderId]) return;

    const es = new EventSource(`/api/events/stream/${orderId}`);
    sourcesRef.current[orderId] = es;

    es.addEventListener('order-event', (e: MessageEvent) => {
      const event: OrderEvent = JSON.parse(e.data);
      setEventsMap((prev) => ({
        ...prev,
        [orderId]: [...(prev[orderId] || []), event],
      }));
    });

    es.onerror = () => {
      es.close();
      delete sourcesRef.current[orderId];
    };
  }, []);

  useEffect(() => {
    return () => {
      Object.values(sourcesRef.current).forEach((es) => es.close());
    };
  }, []);

  const getEvents = useCallback(
    (orderId: string | null): OrderEvent[] => {
      return orderId ? eventsMap[orderId] || [] : [];
    },
    [eventsMap],
  );

  return { subscribe, getEvents };
}
