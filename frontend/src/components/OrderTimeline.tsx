import { OrderEvent, OrderStatus } from '../types/order';

interface OrderTimelineProps {
  events: OrderEvent[];
}

const STATUS_CONFIG: Record<OrderStatus, { label: string; color: string; icon: string }> = {
  ORDER_CREATED: { label: 'Order Created', color: '#3b82f6', icon: '1' },
  PAYMENT_PENDING: { label: 'Payment Pending', color: '#f59e0b', icon: '2' },
  PAYMENT_SUCCESS: { label: 'Payment Success', color: '#10b981', icon: '3' },
  PAYMENT_FAILED: { label: 'Payment Failed', color: '#ef4444', icon: '!' },
  ORDER_SHIPPED: { label: 'Order Shipped', color: '#8b5cf6', icon: '4' },
};

export function OrderTimeline({ events }: OrderTimelineProps) {
  if (events.length === 0) {
    return <div className="timeline-empty">Place an order to see the pipeline in action</div>;
  }

  return (
    <div className="timeline">
      <h2>Order Pipeline</h2>
      <div className="timeline-info">
        Order: <code>{events[0].orderId.substring(0, 8)}...</code> &mdash;{' '}
        {events[0].productName} x{events[0].quantity}
      </div>
      <div className="timeline-steps">
        {events.map((event, i) => {
          const config = STATUS_CONFIG[event.status];
          return (
            <div key={i} className="timeline-step">
              <div className="timeline-connector">
                <div
                  className="timeline-dot"
                  style={{ backgroundColor: config.color }}
                >
                  {config.icon}
                </div>
                {i < events.length - 1 && <div className="timeline-line" />}
              </div>
              <div className="timeline-content">
                <div className="timeline-label" style={{ color: config.color }}>
                  {config.label}
                </div>
                <div className="timeline-time">
                  {new Date(event.timestamp).toLocaleTimeString()}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
