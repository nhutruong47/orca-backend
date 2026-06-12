import { useState } from 'react';
import {
    Check,
    ChevronRight,
    Gem,
    MessagesSquare,
    QrCode,
    Sparkles,
    Smartphone,
    Zap,
    type LucideIcon,
} from 'lucide-react';
import type { PaymentMethod } from '../services/paymentService';
import './UpgradePlanPage.css';

interface Plan {
    id: string;
    name: string;
    eyebrow: string;
    price: string;
    priceNote: string;
    tokenLimit: string;
    model: string;
    featured?: boolean;
    current?: boolean;
    description: string;
    features: string[];
    icon: LucideIcon;
    accent: 'coffee' | 'violet' | 'green' | 'blue';
}

const plans: Plan[] = [
    {
        id: 'starter',
        name: 'Starter',
        eyebrow: 'Gói hiện tại',
        price: '0đ',
        priceNote: 'tháng',
        tokenLimit: 'Xưởng nhỏ',
        model: 'AI quản lý công việc',
        current: true,
        description: 'Dành cho xưởng nhỏ bắt đầu quản lý công việc bằng AI.',
        features: ['AI tạo task từ đơn hàng', 'AI giao việc cho nhân viên', 'Theo dõi tiến độ sản xuất', 'Quản lý đơn hàng và batch', 'Báo cáo vận hành cơ bản'],
        icon: MessagesSquare,
        accent: 'coffee',
    },
    {
        id: 'professional',
        name: 'Professional',
        eyebrow: 'Phổ biến nhất',
        price: '129.000đ',
        priceNote: 'tháng',
        tokenLimit: 'Xưởng tăng trưởng',
        model: 'AI điều phối sản xuất',
        featured: true,
        description: 'Dành cho xưởng đang tăng trưởng cần cảnh báo và tối ưu tiến độ.',
        features: ['Cảnh báo công việc có nguy cơ trễ', 'Cảnh báo thiếu nguyên liệu', 'Phân tích hiệu suất sản xuất', 'Phát hiện điểm nghẽn trong quy trình', 'Đề xuất tối ưu tiến độ và nguồn lực'],
        icon: Sparkles,
        accent: 'violet',
    },
    {
        id: 'enterprise',
        name: 'Enterprise',
        eyebrow: 'Quy mô doanh nghiệp',
        price: '249.000đ',
        priceNote: 'tháng',
        tokenLimit: 'Nhiều xưởng',
        model: 'AI quản lý doanh nghiệp',
        description: 'Dành cho doanh nghiệp nhiều xưởng cần lập kế hoạch và dự báo dài hạn.',
        features: ['Lập kế hoạch sản xuất dài hạn', 'Dự báo nhu cầu và công suất', 'Mô phỏng trước các kịch bản sản xuất', 'Quản lý nhiều xưởng trên một nền tảng', 'Thương hiệu riêng cho doanh nghiệp'],
        icon: Zap,
        accent: 'green',
    },
];

export default function UpgradePlanPage() {
    const [selectedPlanId, setSelectedPlanId] = useState(() => localStorage.getItem('orca-ai-plan') || 'professional');
    const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>(() => {
        const saved = localStorage.getItem('orca-payment-method');
        return saved === 'MOMO' || saved === 'VNPAY' ? saved : 'MOMO';
    });

    const handleSelectPlan = (plan: Plan) => {
        if (plan.current) {
            setSelectedPlanId(plan.id);
            return;
        }
        localStorage.setItem('orca-ai-plan-pending', plan.id);
        localStorage.setItem('orca-payment-method', paymentMethod);
        window.location.href = `/vnpay-mock-checkout?planId=${plan.id}&method=${paymentMethod}`;
    };

    return (
        <div className="upgrade-page">
            <section className="upgrade-header">
                <span className="upgrade-header-badge">
                    <Gem size={15} />
                    ORCA AI
                </span>
                <h1>Nâng cấp gói của bạn</h1>
                <p>Chọn gói AI phù hợp để tối ưu quy trình và nâng cao năng suất nhà máy của bạn.</p>
                <div className="upgrade-billing-toggle" aria-label="Chu kỳ thanh toán">
                    <button className="active" type="button">Hàng tháng</button>
                    <button type="button">Hàng năm <span>Tiết kiệm</span></button>
                </div>
            </section>

            <section className="payment-methods" aria-label="Phương thức thanh toán">
                <button
                    type="button"
                    className={paymentMethod === 'MOMO' ? 'active momo' : 'momo'}
                    onClick={() => setPaymentMethod('MOMO')}
                >
                    <Smartphone size={18} />
                    <span>MoMo QR</span>
                    <small>Quét mã ảo từ điện thoại</small>
                </button>
                <button
                    type="button"
                    className={paymentMethod === 'VNPAY' ? 'active vnpay' : 'vnpay'}
                    onClick={() => setPaymentMethod('VNPAY')}
                >
                    <QrCode size={18} />
                    <span>VNPay QR</span>
                    <small>Mobile banking / ví điện tử</small>
                </button>
            </section>

            <section className="upgrade-grid">
                {plans.map((plan) => {
                    const Icon = plan.icon;
                    const isSelected = selectedPlanId === plan.id;

                    return (
                        <article
                            key={plan.id}
                            className={`plan-card accent-${plan.accent} ${plan.current ? 'current' : ''} ${plan.featured ? 'featured' : ''} ${isSelected ? 'selected' : ''}`}
                        >
                            {plan.featured && <span className="plan-ribbon">Phổ biến</span>}
                            <div className="plan-topline">
                                <div className="plan-icon">
                                    <Icon size={20} />
                                </div>
                                <span>{plan.eyebrow}</span>
                            </div>

                            <h2>{plan.name}</h2>
                            <p className="plan-description">{plan.description}</p>

                            <div className="plan-price">
                                <strong>{plan.price}</strong>
                                <span>{plan.priceNote}</span>
                            </div>

                            <div className="plan-quota">
                                <div>
                                    <span>Hạn mức</span>
                                    <strong>{plan.tokenLimit}</strong>
                                </div>
                                <div>
                                    <span>Model</span>
                                    <strong>{plan.model}</strong>
                                </div>
                            </div>

                            <button
                                type="button"
                                className="plan-action"
                                onClick={() => handleSelectPlan(plan)}
                                disabled={plan.current}
                            >
                                {plan.current ? 'Gói hiện tại' : isSelected ? `Đã chọn ${plan.name}` : `Chọn ${plan.name}`}
                                {!plan.current && <ChevronRight size={16} />}
                            </button>

                            <ul className="plan-features">
                                {plan.features.map((feature) => (
                                    <li key={feature}>
                                        <Check size={15} />
                                        <span>{feature}</span>
                                    </li>
                                ))}
                            </ul>
                        </article>
                    );
                })}
            </section>
        </div>
    );
}
