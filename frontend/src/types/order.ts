export interface OrderEvent {
  orderId: string;
  productName: string;
  quantity: number;
  price: number;
  status: OrderStatus;
  timestamp: number;
}

export type OrderStatus =
  | 'ORDER_CREATED'
  | 'PAYMENT_PENDING'
  | 'PAYMENT_SUCCESS'
  | 'PAYMENT_FAILED'
  | 'ORDER_SHIPPED';
