import { useState, useCallback } from 'react';
import { OrderForm } from './components/OrderForm';
import { OrderTimeline } from './components/OrderTimeline';
import { OrderList } from './components/OrderList';
import { useOrderEvents } from './hooks/useOrderEvents';
import { OrderEvent, OrderStatus } from './types/order';
import './App.css';

interface OrderRecord {
  order: OrderEvent;
  latestStatus: OrderStatus;
}

function App() {
  const [orders, setOrders] = useState<OrderRecord[]>([]);
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);
  const { subscribe, getEvents } = useOrderEvents();

  const events = getEvents(selectedOrderId);

  // Update latest status when events come in
  const latestEvent = events.length > 0 ? events[events.length - 1] : null;
  if (latestEvent) {
    const idx = orders.findIndex((o) => o.order.orderId === latestEvent.orderId);
    if (idx !== -1 && orders[idx].latestStatus !== latestEvent.status) {
      orders[idx] = { ...orders[idx], latestStatus: latestEvent.status };
    }
  }

  const handleOrderCreated = useCallback(
    (order: OrderEvent) => {
      setOrders((prev) => [
        { order, latestStatus: order.status },
        ...prev,
      ]);
      subscribe(order.orderId);
      setSelectedOrderId(order.orderId);
    },
    [subscribe],
  );

  return (
    <div className="app">
      <header className="app-header">
        <h1>Kafka Order Processing</h1>
        <p>Real-time order pipeline visualization</p>
      </header>
      <main className="app-main">
        <div className="left-panel">
          <OrderForm onOrderCreated={handleOrderCreated} />
          <OrderList
            orders={orders}
            selectedOrderId={selectedOrderId}
            onSelect={setSelectedOrderId}
          />
        </div>
        <div className="right-panel">
          <OrderTimeline events={events} />
        </div>
      </main>
    </div>
  );
}

export default App;
