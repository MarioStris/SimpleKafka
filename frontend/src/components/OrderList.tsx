import { OrderEvent, OrderStatus } from '../types/order';

interface OrderInfo {
  order: OrderEvent;
  latestStatus: OrderStatus;
}

interface OrderListProps {
  orders: OrderInfo[];
  selectedOrderId: string | null;
  onSelect: (orderId: string) => void;
}

const STATUS_BADGE: Record<OrderStatus, { label: string; className: string }> = {
  ORDER_CREATED: { label: 'Created', className: 'badge-blue' },
  PAYMENT_PENDING: { label: 'Payment Pending', className: 'badge-yellow' },
  PAYMENT_SUCCESS: { label: 'Payment OK', className: 'badge-green' },
  PAYMENT_FAILED: { label: 'Payment Failed', className: 'badge-red' },
  ORDER_SHIPPED: { label: 'Shipped', className: 'badge-purple' },
};

export function OrderList({ orders, selectedOrderId, onSelect }: OrderListProps) {
  if (orders.length === 0) {
    return <div className="order-list-empty">No orders yet</div>;
  }

  return (
    <div className="order-list">
      <h2>Orders</h2>
      {orders.map(({ order, latestStatus }) => {
        const badge = STATUS_BADGE[latestStatus];
        return (
          <div
            key={order.orderId}
            className={`order-item ${order.orderId === selectedOrderId ? 'selected' : ''}`}
            onClick={() => onSelect(order.orderId)}
          >
            <div className="order-item-header">
              <code>{order.orderId.substring(0, 8)}...</code>
              <span className={`badge ${badge.className}`}>{badge.label}</span>
            </div>
            <div className="order-item-details">
              {order.productName} x{order.quantity} &mdash; ${order.price.toFixed(2)}
            </div>
          </div>
        );
      })}
    </div>
  );
}
