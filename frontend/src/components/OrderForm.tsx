import { useState } from 'react';
import { OrderEvent } from '../types/order';

interface OrderFormProps {
  onOrderCreated: (order: OrderEvent) => void;
}

export function OrderForm({ onOrderCreated }: OrderFormProps) {
  const [productName, setProductName] = useState('MacBook Pro');
  const [quantity, setQuantity] = useState(1);
  const [price, setPrice] = useState(2499.99);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const res = await fetch('/api/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productName, quantity, price }),
      });
      const order: OrderEvent = await res.json();
      onOrderCreated(order);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="order-form" onSubmit={handleSubmit}>
      <h2>Create Order</h2>
      <div className="form-group">
        <label>Product Name</label>
        <input
          type="text"
          value={productName}
          onChange={(e) => setProductName(e.target.value)}
          required
        />
      </div>
      <div className="form-group">
        <label>Quantity</label>
        <input
          type="number"
          min={1}
          value={quantity}
          onChange={(e) => setQuantity(Number(e.target.value))}
          required
        />
      </div>
      <div className="form-group">
        <label>Price ($)</label>
        <input
          type="number"
          min={0}
          step={0.01}
          value={price}
          onChange={(e) => setPrice(Number(e.target.value))}
          required
        />
      </div>
      <button type="submit" disabled={loading}>
        {loading ? 'Placing Order...' : 'Place Order'}
      </button>
    </form>
  );
}
