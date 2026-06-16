import { useEffect, useMemo, useState } from 'react';
import { teamService } from '../services/groupService';
import { interGroupOrderService } from '../services/interGroupOrderService';
import type { Team, InterGroupOrder } from '../types/types';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import './Marketplace.css';

type FactoryProfileTab = 'overview' | 'services' | 'certifications' | 'rfq';
type AvailabilityStatus = 'AVAILABLE' | 'LIMITED' | 'FULLY_BOOKED' | 'UNKNOWN';

type ManufacturingRequest = {
    id: string;
    type: 'Roasting' | 'Packaging' | 'OEM' | 'Quality control';
    title: string;
    coffeeType: string;
    quantity: string;
    deadline: string;
    region: string;
    details: string;
    createdAt: string;
};

type MarketplaceFactory = Team & {
    monthlyCapacity?: string;
    availableCapacity?: string;
    moq?: string;
    onTimeRate?: number;
    avgResponseTime?: string;
    repeatCustomerRate?: number;
    availabilityStatus?: AvailabilityStatus;
    coffeeTypes?: string[];
    services?: string[];
    roasters?: string[];
    packagingMachines?: string[];
    grinders?: string[];
    qcEquipment?: string[];
    certifications?: string[];
    verifiedFactory?: boolean;
    verifiedBusiness?: boolean;
    verifiedAddress?: boolean;
    verifiedCertification?: boolean;
    portfolioProjects?: string[];
    notableCustomers?: string[];
    processedCoffeeLines?: string[];
    reviews?: { author: string; content: string; rating?: number }[];
};

const REGION_OPTIONS = ['Lâm Đồng', 'Đắk Lắk', 'Gia Lai', 'Kon Tum', 'Đồng Nai', 'Bình Dương', 'TP HCM', 'Khác'];
const FACTORY_TYPE_OPTIONS = [
    'Xưởng rang cà phê',
    'Xưởng gia công OEM',
    'Nhà máy chế biến',
    'Hợp tác xã',
    'Doanh nghiệp xuất khẩu',
    'Nhà cung cấp thiết bị',
];
const SPECIALTY_OPTIONS = [
    'Rang cà phê',
    'Gia công OEM',
    'Đóng gói',
    'Xay cà phê',
    'Sản xuất Private Label',
    'QC kiểm định',
    'Xuất khẩu',
    'Cung ứng cà phê nhân',
    'Thiết kế bao bì',
];
const CERTIFICATE_OPTIONS = ['HACCP', 'ISO 22000', 'ISO 9001', 'OCOP', 'FDA', 'Khác'];
const RFQ_TYPE_OPTIONS = ['Rang cà phê', 'Gia công OEM', 'Đóng gói', 'Xay cà phê', 'Private Label', 'Mua cà phê nhân', 'Khác'];
const RFQ_UNIT_OPTIONS = ['kg', 'tấn', 'bao', 'gói'];

const profileTabLabels: Record<FactoryProfileTab, string> = {
    overview: 'Tổng quan',
    services: 'Dịch vụ',
    certifications: 'Chứng nhận',
    rfq: 'Yêu cầu báo giá',
};

const verificationStatusLabel = (status: string) => {
    const labels: Record<string, string> = {
        NOT_SUBMITTED: 'Chưa gửi',
        PENDING: 'Đang chờ quản trị viên duyệt',
        APPROVED: 'Đã xác minh',
        REJECTED: 'Bị từ chối',
    };
    return labels[status] || status;
};

const REQUEST_STORAGE_KEY = 'orca-marketplace-rfqs';

const emptyValue = 'Chưa cập nhật';

const splitMultiValue = (value?: string | string[]) => {
    if (Array.isArray(value)) return value.filter(Boolean).map(item => item.trim()).filter(Boolean);
    if (!value) return [];
    return value.split(/[,;\n]/).map(item => item.trim()).filter(Boolean);
};

const buildCapacityLabel = (team: Team) => {
    if (team.capacityValue && team.capacityUnit) return `${team.capacityValue} ${team.capacityUnit}`;
    return team.capacity || undefined;
};

const normalizeFactory = (team: Team): MarketplaceFactory => ({
    ...team,
    monthlyCapacity: buildCapacityLabel(team),
    services: splitMultiValue(team.specialty),
    availabilityStatus: 'UNKNOWN',
    verifiedFactory: team.verificationStatus === 'APPROVED',
    verifiedBusiness: team.verificationStatus === 'APPROVED' && Boolean(team.businessLicense),
    verifiedAddress: team.verificationStatus === 'APPROVED' && Boolean(team.businessAddress),
    verifiedCertification: team.verificationStatus === 'APPROVED' && Boolean(team.certificationDocument || team.certificates?.length),
    certifications: team.certificates?.length ? team.certificates : splitMultiValue(team.certificationDocument),
});

const getCompletionRate = (factory: MarketplaceFactory) => {
    if (!factory.totalOrders) return undefined;
    return Math.round(((factory.completedOrders || 0) / factory.totalOrders) * 100);
};

const getTrustScore = (factory: MarketplaceFactory) => {
    if (!factory.totalOrders) return undefined;
    if (typeof factory.trustScore === 'number') return factory.trustScore;
    return getCompletionRate(factory);
};

const availabilityCopy = (status?: AvailabilityStatus) => {
    switch (status) {
        case 'AVAILABLE':
            return { label: 'Còn nhận đơn', className: 'available' };
        case 'LIMITED':
            return { label: 'Công suất hạn chế', className: 'limited' };
        case 'FULLY_BOOKED':
            return { label: 'Fully Booked', className: 'booked' };
        default:
            return { label: 'Chưa cập nhật', className: 'unknown' };
    }
};

const displayPercent = (value?: number) => (typeof value === 'number' ? `${value}%` : emptyValue);
const displayText = (value?: string | number | null) => (value || value === 0 ? String(value) : emptyValue);

const loadRequests = (): ManufacturingRequest[] => {
    try {
        const raw = localStorage.getItem(REQUEST_STORAGE_KEY);
        return raw ? JSON.parse(raw) : [];
    } catch {
        return [];
    }
};

export default function MarketplacePage() {
    const { user } = useAuth();
    const navigate = useNavigate();

    const [allTeams, setAllTeams] = useState<Team[]>([]);
    const [myTeams, setMyTeams] = useState<Team[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [searchQuery, setSearchQuery] = useState('');
    const [regionFilter, setRegionFilter] = useState('');
    const [factoryTypeFilter, setFactoryTypeFilter] = useState('');
    const [specialtyFilter, setSpecialtyFilter] = useState('');
    const [minCapacityFilter, setMinCapacityFilter] = useState('');
    const [verifiedFilter, setVerifiedFilter] = useState('');
    const [certificateFilter, setCertificateFilter] = useState('');
    const [selectedFactory, setSelectedFactory] = useState<MarketplaceFactory | null>(null);
    const [activeProfileTab, setActiveProfileTab] = useState<FactoryProfileTab>('overview');
    const [compareIds, setCompareIds] = useState<string[]>([]);

    const [manufacturingRequests, setManufacturingRequests] = useState<ManufacturingRequest[]>([]);

    const [showChatModal, setShowChatModal] = useState(false);
    const [chatTarget, setChatTarget] = useState<MarketplaceFactory | null>(null);
    const [chatDraft, setChatDraft] = useState('');

    const [showOrderModal, setShowOrderModal] = useState(false);
    const [selectedSeller, setSelectedSeller] = useState<Team | null>(null);
    const [buyerTeamId, setBuyerTeamId] = useState('');
    const [rfqTitle, setRfqTitle] = useState('');
    const [rfqRequestType, setRfqRequestType] = useState(RFQ_TYPE_OPTIONS[0]);
    const [rfqProductName, setRfqProductName] = useState('');
    const [rfqQuantity, setRfqQuantity] = useState(1);
    const [rfqUnit, setRfqUnit] = useState(RFQ_UNIT_OPTIONS[0]);
    const [rfqDeadline, setRfqDeadline] = useState('');
    const [rfqBudget, setRfqBudget] = useState('');
    const [rfqQuality, setRfqQuality] = useState('');
    const [rfqPackaging, setRfqPackaging] = useState('');
    const [rfqNote, setRfqNote] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const [showPublishModal, setShowPublishModal] = useState(false);
    const [publishTeamId, setPublishTeamId] = useState('');
    const [pubFactoryType, setPubFactoryType] = useState('');
    const [pubSpecialty, setPubSpecialty] = useState('');
    const [pubCapacityValue, setPubCapacityValue] = useState('');
    const [pubCapacityUnit, setPubCapacityUnit] = useState('kg/tháng');
    const [pubRegion, setPubRegion] = useState('');
    const [pubDescription, setPubDescription] = useState('');
    const [pubFactoryImageUrl, setPubFactoryImageUrl] = useState('');
    const [pubFactoryImages, setPubFactoryImages] = useState<string[]>([]);
    const [pubBusinessLicense, setPubBusinessLicense] = useState('');
    const [pubBusinessAddress, setPubBusinessAddress] = useState('');
    const [pubWebsiteUrl, setPubWebsiteUrl] = useState('');
    const [pubFacebookUrl, setPubFacebookUrl] = useState('');
    const [pubCertificates, setPubCertificates] = useState<string[]>([]);
    const [pubCertificationDocument, setPubCertificationDocument] = useState('');
    const [publishing, setPublishing] = useState(false);
    const editingPublishedTeam = myTeams.find(team => team.id === publishTeamId && team.isPublished);
    const selectedPublishTeam = myTeams.find(team => team.id === publishTeamId);
    const publishVerificationStatus = selectedPublishTeam?.verificationStatus || 'NOT_SUBMITTED';

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [teamsAll, teamsMine] = await Promise.all([
                    teamService.getAllTeams(),
                    teamService.getMyTeams(),
                ]);
                const publishedTeams = teamsAll.filter(t => t.isPublished);
                const ownedTeams = teamsMine.filter(t => t.ownerId === user?.id);
                setAllTeams(publishedTeams);
                setMyTeams(ownedTeams);
                if (ownedTeams.length > 0) {
                    setBuyerTeamId(ownedTeams[0].id);
                    setPublishTeamId(ownedTeams[0].id);
                }
                setManufacturingRequests(loadRequests());
            } catch (err) {
                console.error('Failed to load marketplace', err);
                setError('Không thể tải dữ liệu thị trường.');
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, [user]);

    const factories = useMemo(() => allTeams.map(normalizeFactory), [allTeams]);

    const displayedFactories = useMemo(() => {
        const q = searchQuery.trim().toLowerCase();
        const minCapacity = Number(minCapacityFilter) || 0;
        return factories.filter(factory => {
            const searchable = [
                factory.name,
                factory.region,
                factory.factoryType,
                factory.specialty,
                factory.description,
                ...(factory.services || []),
                ...(factory.coffeeTypes || []),
            ].filter(Boolean).join(' ').toLowerCase();

            const matchesSearch = !q || searchable.includes(q);
            const matchesRegion = !regionFilter || factory.region === regionFilter;
            const matchesType = !factoryTypeFilter || factory.factoryType === factoryTypeFilter;
            const matchesSpecialty = !specialtyFilter || splitMultiValue(factory.specialty).includes(specialtyFilter);
            const matchesCapacity = !minCapacity || (factory.capacityValue || 0) >= minCapacity;
            const matchesVerified = !verifiedFilter
                || (verifiedFilter === 'verified' ? factory.verificationStatus === 'APPROVED' : factory.verificationStatus !== 'APPROVED');
            const matchesCertificate = !certificateFilter
                || (certificateFilter === 'has' ? Boolean(factory.certifications?.length) : !factory.certifications?.length);

            return matchesSearch && matchesRegion && matchesType && matchesSpecialty && matchesCapacity && matchesVerified && matchesCertificate;
        });
    }, [certificateFilter, factories, factoryTypeFilter, minCapacityFilter, regionFilter, searchQuery, specialtyFilter, verifiedFilter]);

    const selectedCompareFactories = factories.filter(factory => compareIds.includes(factory.id));
    const myPublishedTeams = myTeams.filter(team => team.isPublished);

    const fillPublishForm = (team: Team) => {
        const images = team.factoryImages?.length ? team.factoryImages : team.factoryImageUrl ? [team.factoryImageUrl] : [];
        setPublishTeamId(team.id);
        setPubFactoryType(team.factoryType || '');
        setPubSpecialty(team.specialty || '');
        setPubCapacityValue(team.capacityValue ? String(team.capacityValue) : '');
        setPubCapacityUnit(team.capacityUnit || 'kg/tháng');
        setPubRegion(team.region || '');
        setPubDescription(team.description || '');
        setPubFactoryImageUrl(team.factoryImageUrl || images[0] || '');
        setPubFactoryImages(images);
        setPubBusinessLicense(team.businessLicense || '');
        setPubBusinessAddress(team.businessAddress || '');
        setPubWebsiteUrl(team.websiteUrl || '');
        setPubFacebookUrl(team.facebookUrl || '');
        setPubCertificates(team.certificates || []);
        setPubCertificationDocument(team.certificationDocument || '');
    };

    const openPublishModal = () => {
        if (myTeams.length === 0) {
            alert('Bạn cần tạo ít nhất 1 nhóm xưởng trước khi đăng tải.');
            navigate('/groups');
            return;
        }
        fillPublishForm(myTeams[0]);
        setShowPublishModal(true);
    };

    const openEditPublishedTeam = (team: Team) => {
        fillPublishForm(team);
        setShowPublishModal(true);
    };

    const handleFactoryImageFile = (event: React.ChangeEvent<HTMLInputElement>) => {
        const files = Array.from(event.target.files || []);
        if (files.length === 0) return;
        if (pubFactoryImages.length + files.length > 10) {
            alert('Chỉ được tải tối đa 10 ảnh xưởng.');
            event.target.value = '';
            return;
        }
        const invalid = files.find(file => !['image/jpeg', 'image/png', 'image/webp'].includes(file.type));
        if (invalid) {
            alert('Ảnh xưởng chỉ hỗ trợ JPG, PNG hoặc WEBP.');
            event.target.value = '';
            return;
        }
        const tooLarge = files.find(file => file.size > 5 * 1024 * 1024);
        if (tooLarge) {
            alert('Mỗi ảnh xưởng tối đa 5MB.');
            event.target.value = '';
            return;
        }
        Promise.all(files.map(file => new Promise<string>((resolve) => {
            const reader = new FileReader();
            reader.onload = () => resolve(String(reader.result || ''));
            reader.readAsDataURL(file);
        }))).then(images => {
            setPubFactoryImages(current => {
                const next = [...current, ...images].slice(0, 10);
                setPubFactoryImageUrl(next[0] || '');
                return next;
            });
        });
        event.target.value = '';
    };

    const handleDocumentFile = (event: React.ChangeEvent<HTMLInputElement>, setter: (value: string) => void) => {
        const file = event.target.files?.[0];
        if (!file) return;
        if (!['application/pdf', 'image/jpeg', 'image/png'].includes(file.type)) {
            alert('Tài liệu chỉ hỗ trợ PDF, JPG hoặc PNG.');
            event.target.value = '';
            return;
        }
        if (file.size > 10 * 1024 * 1024) {
            alert('Tài liệu tối đa 10MB.');
            event.target.value = '';
            return;
        }
        const reader = new FileReader();
        reader.onload = () => setter(String(reader.result || ''));
        reader.readAsDataURL(file);
        event.target.value = '';
    };

    const handleCompareToggle = (factoryId: string) => {
        setCompareIds(current => {
            if (current.includes(factoryId)) return current.filter(id => id !== factoryId);
            if (current.length >= 4) {
                alert('Chỉ có thể so sánh tối đa 4 xưởng.');
                return current;
            }
            return [...current, factoryId];
        });
    };

    const openChat = (factory: MarketplaceFactory) => {
        setChatTarget(factory);
        setChatDraft('');
        setShowChatModal(true);
    };

    const handleSaveChatDraft = () => {
        setShowChatModal(false);
        alert('Đã lưu nội dung trao đổi. Trò chuyện thời gian thực trên thị trường cần backend hội thoại để gửi trực tiếp cho xưởng.');
    };

    const handleOrderClick = (seller: Team) => {
        if (myTeams.length === 0) {
            alert('Bạn cần đăng nhập và có ít nhất 1 nhóm/xưởng để gửi RFQ.');
            navigate('/groups');
            return;
        }
        setSelectedSeller(seller);
        setRfqTitle('');
        setRfqRequestType(RFQ_TYPE_OPTIONS[0]);
        setRfqProductName('');
        setRfqQuantity(1);
        setRfqUnit(RFQ_UNIT_OPTIONS[0]);
        setRfqDeadline('');
        setRfqBudget('');
        setRfqQuality('');
        setRfqPackaging('');
        setRfqNote('');
        setShowOrderModal(true);
    };

    const handleSubmitOrder = async (event: React.FormEvent) => {
        event.preventDefault();
        if (!buyerTeamId || !rfqTitle.trim() || !rfqProductName.trim() || !selectedSeller) return;
        if (rfqQuantity <= 0) {
            alert('Số lượng phải lớn hơn 0.');
            return;
        }
        if (rfqDeadline && new Date(rfqDeadline) <= new Date()) {
            alert('Deadline mong muốn phải là ngày trong tương lai.');
            return;
        }

        try {
            setSubmitting(true);
            const detailLines = [
                `Loại nhu cầu: ${rfqRequestType}`,
                `Sản phẩm mong muốn: ${rfqProductName}`,
                `Số lượng: ${rfqQuantity} ${rfqUnit}`,
                rfqBudget ? `Ngân sách dự kiến: ${rfqBudget}` : '',
                rfqQuality ? `Yêu cầu chất lượng: ${rfqQuality}` : '',
                rfqPackaging ? `Yêu cầu đóng gói: ${rfqPackaging}` : '',
                rfqNote ? `Ghi chú: ${rfqNote}` : '',
            ].filter(Boolean).join('\n');
            const dto: Partial<InterGroupOrder> = {
                buyerTeamId,
                sellerTeamId: selectedSeller.id,
                title: rfqTitle,
                description: detailLines,
                quantity: rfqQuantity,
                deadline: rfqDeadline,
            };
            await interGroupOrderService.placeOrder(dto);
            setShowOrderModal(false);
            const request: ManufacturingRequest = {
                id: crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}`,
                type: rfqRequestType as ManufacturingRequest['type'],
                title: rfqTitle,
                coffeeType: rfqProductName,
                quantity: `${rfqQuantity} ${rfqUnit}`,
                deadline: rfqDeadline,
                region: selectedSeller.region || '',
                details: detailLines,
                createdAt: new Date().toISOString(),
            };
            const next = [request, ...manufacturingRequests];
            setManufacturingRequests(next);
            localStorage.setItem(REQUEST_STORAGE_KEY, JSON.stringify(next));
            alert('Đã gửi RFQ/yêu cầu gia công. Chuyển sang trang đơn liên xưởng để theo dõi.');
            navigate('/orders');
        } catch {
            alert('Không thể gửi RFQ. Vui lòng thử lại.');
        } finally {
            setSubmitting(false);
        }
    };

    const handlePublish = async (event: React.FormEvent) => {
        event.preventDefault();
        if (!publishTeamId) return;
        const capacityValue = Number(pubCapacityValue);

        if (!pubFactoryType || !pubRegion || !pubBusinessAddress.trim() || !pubSpecialty.trim() || !capacityValue || capacityValue <= 0 || !pubCapacityUnit || pubDescription.trim().length < 30 || pubFactoryImages.length === 0) {
            alert('Vui lòng nhập đủ: loại hình, khu vực, địa chỉ xưởng, chuyên môn, công suất > 0, mô tả tối thiểu 30 ký tự và ít nhất 1 ảnh xưởng.');
            return;
        }
        if (!pubBusinessLicense.trim()) {
            alert('Giấy phép kinh doanh là bắt buộc khi gửi xác minh hồ sơ xưởng.');
            return;
        }

        try {
            setPublishing(true);
            await teamService.advertise(publishTeamId, {
                factoryType: pubFactoryType,
                specialty: pubSpecialty,
                capacity: `${capacityValue} ${pubCapacityUnit}`,
                capacityValue,
                capacityUnit: pubCapacityUnit,
                region: pubRegion,
                description: pubDescription,
                factoryImageUrl: pubFactoryImages[0] || pubFactoryImageUrl,
                factoryImages: pubFactoryImages,
            } as Partial<Team>);
            await teamService.submitVerification(publishTeamId, {
                businessLicense: pubBusinessLicense,
                businessAddress: pubBusinessAddress,
                websiteUrl: pubWebsiteUrl,
                facebookUrl: pubFacebookUrl,
                certificates: pubCertificates,
                certificationDocument: pubCertificationDocument,
            } as Partial<Team>);
            setShowPublishModal(false);
            alert('Xưởng đã được lưu và hồ sơ xác minh đã gửi Admin duyệt.');
            const [teamsAll, teamsMine] = await Promise.all([teamService.getAllTeams(), teamService.getMyTeams()]);
            setAllTeams(teamsAll.filter(t => t.isPublished));
            setMyTeams(teamsMine.filter(t => t.ownerId === user?.id));
        } catch {
            alert('Không thể đăng xưởng. Vui lòng thử lại.');
        } finally {
            setPublishing(false);
        }
    };

    const handleUnpublish = async (teamId: string) => {
        if (!confirm('Gỡ xưởng này khỏi thị trường?')) return;
        try {
            await teamService.unpublish(teamId);
            alert('Đã gỡ xưởng khỏi thị trường.');
            const [teamsAll, teamsMine] = await Promise.all([teamService.getAllTeams(), teamService.getMyTeams()]);
            setAllTeams(teamsAll.filter(t => t.isPublished));
            setMyTeams(teamsMine.filter(t => t.ownerId === user?.id));
        } catch {
            alert('Không thể gỡ xưởng.');
        }
    };

    const renderMetric = (label: string, value?: string | number) => (
        <div className="mp-capacity-metric">
            <span>{label}</span>
            <strong>{displayText(value)}</strong>
        </div>
    );

    const renderVerification = (factory: MarketplaceFactory) => {
        const badges = [
            { label: 'Xưởng đã xác minh', active: factory.verifiedFactory },
            { label: 'Doanh nghiệp đã xác minh', active: factory.verifiedBusiness },
            { label: 'Địa chỉ đã xác minh', active: factory.verifiedAddress },
            { label: 'Chứng nhận đã xác minh', active: factory.verifiedCertification },
        ];
        const hasAny = badges.some(badge => badge.active);
        if (!hasAny) return <p className="mp-empty-inline">Chưa xác minh</p>;
        return (
            <div className="mp-verification-list">
                {badges.map(badge => (
                    <span className={badge.active ? 'verified' : ''} key={badge.label}>
                        <span className="material-symbols-outlined">{badge.active ? 'verified' : 'radio_button_unchecked'}</span>
                        {badge.label}
                    </span>
                ))}
            </div>
        );
    };

    const renderProfileTab = (factory: MarketplaceFactory) => {
        switch (activeProfileTab) {
            case 'services':
                return (
                    <>
                        <div className="mp-profile-grid">
                            {renderMetric('Loại hình', factory.factoryType)}
                            {renderMetric('Chuyên môn', factory.services?.join(', ') || factory.specialty)}
                            {renderMetric('Công suất', factory.monthlyCapacity)}
                            {renderMetric('Khu vực', factory.region)}
                        </div>
                        <div className="mp-detail-tags">
                            {(factory.services || []).map(service => <span key={service}>{service}</span>)}
                            {(!factory.services || factory.services.length === 0) && <span>Chưa cập nhật dịch vụ</span>}
                        </div>
                    </>
                );
            case 'certifications':
                return (
                    <>
                        {renderVerification(factory)}
                        <div className="mp-detail-tags">
                            {(factory.certifications || []).map(cert => <span key={cert}>{cert}</span>)}
                            {(!factory.certifications || factory.certifications.length === 0) && <span>Chưa cập nhật chứng nhận</span>}
                        </div>
                    </>
                );
            case 'rfq':
                return (
                    <div className="mp-profile-overview">
                        <p>Gửi RFQ để mô tả sản phẩm, số lượng, deadline và yêu cầu chất lượng. Xưởng sẽ phản hồi trên luồng đơn liên xưởng.</p>
                        <button
                            className="mp-submit-btn"
                            disabled={factory.ownerId === user?.id}
                            onClick={() => handleOrderClick(factory)}
                        >
                            {factory.ownerId === user?.id ? 'Xưởng của bạn' : 'Gửi RFQ'}
                        </button>
                    </div>
                );
            default:
                return (
                    <div className="mp-profile-overview">
                        <p>{factory.description || 'Chưa cập nhật mô tả xưởng'}</p>
                        <div className="mp-profile-grid">
                            {renderMetric('Loại hình', factory.factoryType)}
                            {renderMetric('Khu vực', factory.region)}
                            {renderMetric('Địa chỉ xưởng', factory.businessAddress)}
                            {renderMetric('Công suất', factory.monthlyCapacity)}
                            {renderMetric('Đơn đã hoàn thành', factory.totalOrders ? factory.completedOrders : undefined)}
                            {renderMetric('Điểm uy tín', factory.totalOrders ? displayPercent(getTrustScore(factory)) : undefined)}
                        </div>
                    </div>
                );
        }
    };

    if (loading) {
        return (
            <div className="mp-body" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
                <div className="btn-spinner" />
            </div>
        );
    }

    return (
        <div className="mp-body mp-market-style mp-manufacturing-market">
            <Sidebar />

            <header className="mp-topbar">
                <div className="mp-top-search">
                    <span className="material-symbols-outlined">search</span>
                    <input
                        type="text"
                        placeholder="Tìm theo xưởng, khu vực, chuyên môn, dịch vụ..."
                        value={searchQuery}
                        onChange={event => setSearchQuery(event.target.value)}
                    />
                </div>
                <div className="mp-top-actions">
                    <button aria-label="Đơn liên xưởng" onClick={() => navigate('/orders')}>
                        <span className="material-symbols-outlined">receipt_long</span>
                    </button>
                    <button aria-label="Bộ lọc" onClick={() => document.getElementById('mp-filters')?.scrollIntoView({ behavior: 'smooth', block: 'center' })}>
                        <span className="material-symbols-outlined">tune</span>
                    </button>
                    <div className="mp-user-avatar">{(user?.username || user?.fullName || 'O').charAt(0).toUpperCase()}</div>
                </div>
            </header>

            <main className="mp-main">
                <section className="mp-market-hero">
                    <div>
                        <span className="mp-verified">
                            <span className="material-symbols-outlined">precision_manufacturing</span>
                            B2B Manufacturing Marketplace
                        </span>
                        <h1>Thị trường xưởng cà phê</h1>
                        <p>Tìm xưởng phù hợp để rang, gia công, đóng gói và sản xuất cà phê theo yêu cầu.</p>
                        <div className="mp-hero-buttons">
                            <button onClick={() => document.getElementById('mp-partners')?.scrollIntoView({ behavior: 'smooth' })}>Tìm xưởng phù hợp</button>
                            <button onClick={() => navigate('/orders')}>Theo dõi đơn liên xưởng</button>
                        </div>
                    </div>
                </section>

                <section id="mp-filters" className="mp-filter-panel">
                    <div className="mp-filter-grid">
                        <div className="mp-form-group">
                            <label>Khu vực</label>
                            <select value={regionFilter} onChange={event => setRegionFilter(event.target.value)}>
                                <option value="">Tất cả khu vực</option>
                                {REGION_OPTIONS.map(option => <option key={option} value={option}>{option}</option>)}
                            </select>
                        </div>
                        <div className="mp-form-group">
                            <label>Loại hình xưởng</label>
                            <select value={factoryTypeFilter} onChange={event => setFactoryTypeFilter(event.target.value)}>
                                <option value="">Tất cả loại hình</option>
                                {FACTORY_TYPE_OPTIONS.map(option => <option key={option} value={option}>{option}</option>)}
                            </select>
                        </div>
                        <div className="mp-form-group">
                            <label>Chuyên môn</label>
                            <select value={specialtyFilter} onChange={event => setSpecialtyFilter(event.target.value)}>
                                <option value="">Tất cả chuyên môn</option>
                                {SPECIALTY_OPTIONS.map(option => <option key={option} value={option}>{option}</option>)}
                            </select>
                        </div>
                        <div className="mp-form-group">
                            <label>Công suất tối thiểu</label>
                            <input type="number" min="0" value={minCapacityFilter} onChange={event => setMinCapacityFilter(event.target.value)} placeholder="VD: 500" />
                        </div>
                        <div className="mp-form-group">
                            <label>Xác minh</label>
                            <select value={verifiedFilter} onChange={event => setVerifiedFilter(event.target.value)}>
                                <option value="">Tất cả</option>
                                <option value="verified">Đã xác minh</option>
                                <option value="unverified">Chưa xác minh</option>
                            </select>
                        </div>
                        <div className="mp-form-group">
                            <label>Chứng nhận</label>
                            <select value={certificateFilter} onChange={event => setCertificateFilter(event.target.value)}>
                                <option value="">Tất cả</option>
                                <option value="has">Có chứng nhận</option>
                                <option value="none">Chưa có chứng nhận</option>
                            </select>
                        </div>
                    </div>
                    <div className="mp-filter-footer">
                        <span>{displayedFactories.length} xưởng phù hợp</span>
                        <button type="button" onClick={() => {
                            setRegionFilter('');
                            setFactoryTypeFilter('');
                            setSpecialtyFilter('');
                            setMinCapacityFilter('');
                            setVerifiedFilter('');
                            setCertificateFilter('');
                        }}>Xóa lọc</button>
                    </div>
                </section>

                {myPublishedTeams.length > 0 && (
                    <section className="mp-published-panel">
                        <h3><span className="material-symbols-outlined">storefront</span>Xưởng của bạn trên thị trường</h3>
                        <div className="mp-my-published-list">
                            {myPublishedTeams.map(team => (
                                <div key={team.id} className="mp-my-pub-item">
                                    <div>
                                        <strong>{team.name}</strong>
                                        <span className="mp-pub-badge">Đang hiển thị</span>
                                    </div>
                                    <button className="mp-edit-pub-btn" onClick={() => openEditPublishedTeam(team)}>Chỉnh sửa</button>
                                    <button className="mp-unpub-btn" onClick={() => handleUnpublish(team.id)}>Gỡ xuống</button>
                                </div>
                            ))}
                        </div>
                    </section>
                )}

                <section id="mp-partners" className="mp-partner-section">
                    <div className="mp-section-title-row">
                        <div>
                            <h2>Khám phá xưởng</h2>
                            <p>Đánh giá xưởng theo năng lực sản xuất, khả dụng và độ tin cậy vận hành.</p>
                        </div>
                        <button onClick={openPublishModal}>
                            Đăng xưởng
                            <span className="material-symbols-outlined">add_business</span>
                        </button>
                    </div>

                    {error && <div className="mp-error">{error}</div>}

                    {displayedFactories.length === 0 ? (
                        <div className="mp-empty mp-styled-empty">
                            <span className="material-symbols-outlined">factory</span>
                            <h3>Chưa có xưởng thật nào trên thị trường</h3>
                            <p>Đăng xưởng của bạn để bắt đầu nhận yêu cầu gia công. ORCA không hiển thị dữ liệu xưởng giả trong discovery.</p>
                            <button className="mp-publish-btn" onClick={openPublishModal}>Đăng xưởng ngay</button>
                        </div>
                    ) : (
                        <div className="mp-factory-grid">
                            {displayedFactories.map(factory => {
                                const availability = availabilityCopy(factory.availabilityStatus);
                                const isOwnFactory = factory.ownerId === user?.id;
                                return (
                                    <article key={factory.id} className="mp-factory-card">
                                        <div className="mp-factory-card-head">
                                            <div>
                                                <h3>{factory.name}{isOwnFactory && <span className="mp-own-factory-badge">Xưởng của bạn</span>}</h3>
                                                <p>{displayText(factory.factoryType)} · {displayText(factory.region)} · {displayText(factory.specialty)}</p>
                                            </div>
                                            <span className={`mp-availability ${availability.className}`}>{availability.label}</span>
                                        </div>
                                        {(factory.factoryImageUrl || factory.factoryImages?.[0]) && (
                                            <div className="mp-factory-image">
                                                <img src={factory.factoryImageUrl || factory.factoryImages?.[0]} alt={`Ảnh xưởng ${factory.name}`} />
                                            </div>
                                        )}
                                        <div className="mp-factory-trust">
                                            <span>Uy tín</span>
                                            <strong>{factory.totalOrders ? displayPercent(getTrustScore(factory)) : emptyValue}</strong>
                                        </div>
                                        <div className="mp-card-capacity-grid">
                                            {renderMetric('Công suất', factory.monthlyCapacity)}
                                            {renderMetric('Còn trống', factory.availableCapacity)}
                                            {renderMetric('MOQ', factory.moq)}
                                            {renderMetric('Hoàn thành', displayPercent(getCompletionRate(factory)))}
                                            {renderMetric('Đúng hạn', displayPercent(factory.onTimeRate))}
                                        </div>
                                        <div className="mp-verification-strip">
                                            {factory.verifiedFactory ? <span>Xưởng đã xác minh</span> : <span>Chưa xác minh</span>}
                                            {factory.verifiedCertification && <span>Chứng nhận</span>}
                                        </div>
                                        <div className="mp-factory-actions">
                                            <label>
                                                <input
                                                    type="checkbox"
                                                    checked={compareIds.includes(factory.id)}
                                                    onChange={() => handleCompareToggle(factory.id)}
                                                />
                                                So sánh
                                            </label>
                                            <button onClick={() => { setSelectedFactory(factory); setActiveProfileTab('overview'); }}>Xem chi tiết</button>
                                            {isOwnFactory ? (
                                                <>
                                                    <button onClick={() => openEditPublishedTeam(factory)}>Cập nhật</button>
                                                    <button disabled>Đã đăng</button>
                                                </>
                                            ) : (
                                                <>
                                                    <button onClick={() => openChat(factory)}>Trao đổi trước</button>
                                                    <button onClick={() => handleOrderClick(factory)}>Gửi yêu cầu</button>
                                                </>
                                            )}
                                        </div>
                                    </article>
                                );
                            })}
                        </div>
                    )}
                </section>

                {selectedCompareFactories.length > 0 && (
                    <section className="mp-compare-panel">
                        <div className="mp-section-title-row">
                            <div>
                                <h2>So sánh xưởng</h2>
                                <p>So sánh 2-4 xưởng theo chỉ số năng lực chính.</p>
                            </div>
                            <button onClick={() => setCompareIds([])}>Xóa so sánh</button>
                        </div>
                        <div className="mp-compare-table-wrap">
                            <table className="mp-compare-table">
                                <thead>
                                    <tr>
                                        <th>Chỉ số</th>
                                        {selectedCompareFactories.map(factory => <th key={factory.id}>{factory.name}</th>)}
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr><td>Công suất</td>{selectedCompareFactories.map(factory => <td key={factory.id}>{displayText(factory.monthlyCapacity)}</td>)}</tr>
                                    <tr><td>MOQ</td>{selectedCompareFactories.map(factory => <td key={factory.id}>{displayText(factory.moq)}</td>)}</tr>
                                    <tr><td>Giá</td>{selectedCompareFactories.map(factory => <td key={factory.id}>{emptyValue}</td>)}</tr>
                                    <tr><td>Uy tín</td>{selectedCompareFactories.map(factory => <td key={factory.id}>{displayPercent(getTrustScore(factory))}</td>)}</tr>
                                    <tr><td>Đúng hạn</td>{selectedCompareFactories.map(factory => <td key={factory.id}>{displayPercent(factory.onTimeRate)}</td>)}</tr>
                                    <tr><td>Chứng nhận</td>{selectedCompareFactories.map(factory => <td key={factory.id}>{factory.certifications?.join(', ') || emptyValue}</td>)}</tr>
                                </tbody>
                            </table>
                        </div>
                    </section>
                )}

                <section className="mp-open-requests">
                    <div className="mp-section-title-row">
                        <div>
                            <h2>Yêu cầu gia công đang mở</h2>
                            <p>Nhu cầu rang, đóng gói, OEM và kiểm định đang mở để xưởng gửi báo giá.</p>
                        </div>
                    </div>

                    {manufacturingRequests.length === 0 ? (
                        <div className="mp-empty mp-styled-empty">
                            <span className="material-symbols-outlined">request_quote</span>
                            <h3>Chưa có nhu cầu sản xuất đang mở</h3>
                            <p>Các nhu cầu gia công đang mở sẽ hiển thị tại đây khi có dữ liệu mới.</p>
                        </div>
                    ) : (
                        <div className="mp-request-grid">
                            {manufacturingRequests.map(request => (
                                <article className="mp-request-card" key={request.id}>
                                    <span>{request.type}</span>
                                    <h3>{request.title}</h3>
                                    <dl>
                                        <div><dt>Cà phê</dt><dd>{displayText(request.coffeeType)}</dd></div>
                                        <div><dt>Sản lượng</dt><dd>{displayText(request.quantity)}</dd></div>
                                        <div><dt>Deadline</dt><dd>{request.deadline ? new Date(request.deadline).toLocaleDateString('vi-VN') : emptyValue}</dd></div>
                                        <div><dt>Khu vực</dt><dd>{displayText(request.region)}</dd></div>
                                    </dl>
                                    <p>{request.details || 'Chưa cập nhật yêu cầu chi tiết'}</p>
                                </article>
                            ))}
                        </div>
                    )}
                </section>

                <section className="mp-cta">
                    <h2>Đưa xưởng của bạn vào mạng lưới sản xuất ORCA</h2>
                    <p>Hồ sơ năng lực càng đầy đủ, doanh nghiệp càng dễ đánh giá và tạo yêu cầu gia công minh bạch.</p>
                    <button onClick={openPublishModal}>Đăng ký trở thành đối tác sản xuất</button>
                </section>

                <footer className="mp-showcase-footer">
                    <span>ORCA</span>
                    <p>© 2026 Coffee Manufacturing Network</p>
                    <div>
                        <a href="#">Điều khoản</a>
                        <a href="#">Bảo mật</a>
                        <a href="#">Hỗ trợ đối tác</a>
                    </div>
                </footer>
            </main>

            {selectedFactory && (
                <div className="mp-modal-overlay" onClick={() => setSelectedFactory(null)}>
                    <div className="mp-workshop-detail mp-profile-detail" onClick={event => event.stopPropagation()}>
                        <button className="mp-detail-close" onClick={() => setSelectedFactory(null)}>
                            <span className="material-symbols-outlined">close</span>
                        </button>
                        <aside className="mp-profile-side">
                            <span className={`mp-availability ${availabilityCopy(selectedFactory.availabilityStatus).className}`}>
                                {availabilityCopy(selectedFactory.availabilityStatus).label}
                            </span>
                            {selectedFactory.factoryImageUrl && (
                                <div className="mp-profile-image">
                                    <img src={selectedFactory.factoryImageUrl} alt={`Ảnh xưởng ${selectedFactory.name}`} />
                                </div>
                            )}
                            <h2>{selectedFactory.name}</h2>
                            <p>{displayText(selectedFactory.region)}</p>
                            <div className="mp-profile-side-metrics">
                                {renderMetric('Độ tin cậy', selectedFactory.totalOrders ? displayPercent(getTrustScore(selectedFactory)) : undefined)}
                                {renderMetric('Công suất', selectedFactory.monthlyCapacity)}
                                {renderMetric('Loại hình', selectedFactory.factoryType)}
                            </div>
                            {renderVerification(selectedFactory)}
                        </aside>
                        <div className="mp-detail-content">
                            <div className="mp-profile-tabs">
                                {(['overview', 'services', 'certifications', 'rfq'] as FactoryProfileTab[]).map(tab => (
                                    <button
                                        key={tab}
                                        className={activeProfileTab === tab ? 'active' : ''}
                                        onClick={() => setActiveProfileTab(tab)}
                                    >
                                        {profileTabLabels[tab]}
                                    </button>
                                ))}
                            </div>
                            {renderProfileTab(selectedFactory)}
                            <div className="mp-detail-actions">
                                <button disabled={selectedFactory.ownerId === user?.id} onClick={() => openChat(selectedFactory)}>Trao đổi trước</button>
                                <button disabled={selectedFactory.ownerId === user?.id} onClick={() => { const factory = selectedFactory; setSelectedFactory(null); handleOrderClick(factory); }}>
                                    {selectedFactory.ownerId === user?.id ? 'Xưởng của bạn' : 'Gửi RFQ'}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {showChatModal && chatTarget && (
                <div className="mp-modal-overlay" onClick={() => setShowChatModal(false)}>
                    <div className="mp-modal" onClick={event => event.stopPropagation()}>
                        <div className="mp-modal-header">
                            <h2>Trao đổi trước với {chatTarget.name}</h2>
                            <button className="mp-modal-close" onClick={() => setShowChatModal(false)}>×</button>
                        </div>
                        <p className="mp-modal-note">Luồng đúng: trao đổi nhu cầu trước, sau đó mới tạo RFQ hoặc yêu cầu gia công.</p>
                        <div className="mp-form-group">
                            <label>Nội dung trao đổi</label>
                            <textarea
                                rows={5}
                                placeholder="Ví dụ: Tôi cần rang 2 tấn Arabica, deadline 20/08, cần báo giá theo profile medium roast..."
                                value={chatDraft}
                                onChange={event => setChatDraft(event.target.value)}
                            />
                        </div>
                        <div className="mp-modal-actions">
                            <button type="button" className="mp-cancel-btn" onClick={() => setShowChatModal(false)}>Để sau</button>
                            <button type="button" className="mp-submit-btn" onClick={handleSaveChatDraft}>Lưu trao đổi</button>
                        </div>
                    </div>
                </div>
            )}

            {showOrderModal && selectedSeller && (
                <div className="mp-modal-overlay" onClick={() => setShowOrderModal(false)}>
                    <div className="mp-modal" onClick={event => event.stopPropagation()}>
                        <div className="mp-modal-header">
                            <h2>Gửi RFQ</h2>
                            <button className="mp-modal-close" onClick={() => setShowOrderModal(false)}>×</button>
                        </div>
                        <div className="mp-modal-seller">Xưởng nhận RFQ: <strong>{selectedSeller.name}</strong></div>
                        <form onSubmit={handleSubmitOrder}>
                            <div className="mp-form-group">
                                <label>Xưởng của bạn</label>
                                <select value={buyerTeamId} onChange={event => setBuyerTeamId(event.target.value)} required>
                                    {myTeams.map(team => <option key={team.id} value={team.id}>{team.name}</option>)}
                                </select>
                            </div>
                            <div className="mp-form-group">
                                <label>Tên yêu cầu</label>
                                <input value={rfqTitle} onChange={event => setRfqTitle(event.target.value)} placeholder="VD: Báo giá rang 2 tấn Arabica" required />
                            </div>
                            <div className="mp-form-row">
                                <div className="mp-form-group">
                                    <label>Loại nhu cầu</label>
                                    <select value={rfqRequestType} onChange={event => setRfqRequestType(event.target.value)} required>
                                        {RFQ_TYPE_OPTIONS.map(option => <option key={option} value={option}>{option}</option>)}
                                    </select>
                                </div>
                                <div className="mp-form-group">
                                    <label>Sản phẩm mong muốn</label>
                                    <input value={rfqProductName} onChange={event => setRfqProductName(event.target.value)} placeholder="VD: Arabica rang medium" required />
                                </div>
                            </div>
                            <div className="mp-form-row">
                                <div className="mp-form-group">
                                    <label>Số lượng</label>
                                    <input type="number" min="1" value={rfqQuantity} onChange={event => setRfqQuantity(parseInt(event.target.value) || 1)} required />
                                </div>
                                <div className="mp-form-group">
                                    <label>Đơn vị</label>
                                    <select value={rfqUnit} onChange={event => setRfqUnit(event.target.value)} required>
                                        {RFQ_UNIT_OPTIONS.map(option => <option key={option} value={option}>{option}</option>)}
                                    </select>
                                </div>
                            </div>
                            <div className="mp-form-row">
                                <div className="mp-form-group">
                                    <label>Deadline mong muốn</label>
                                    <input type="datetime-local" value={rfqDeadline} onChange={event => setRfqDeadline(event.target.value)} />
                                </div>
                                <div className="mp-form-group">
                                    <label>Ngân sách dự kiến</label>
                                    <input value={rfqBudget} onChange={event => setRfqBudget(event.target.value)} placeholder="Tùy chọn" />
                                </div>
                            </div>
                            <div className="mp-form-group">
                                <label>Yêu cầu chất lượng</label>
                                <textarea rows={2} value={rfqQuality} onChange={event => setRfqQuality(event.target.value)} placeholder="Tùy chọn: tiêu chuẩn, profile rang, độ ẩm..." />
                            </div>
                            <div className="mp-form-group">
                                <label>Yêu cầu đóng gói</label>
                                <textarea rows={2} value={rfqPackaging} onChange={event => setRfqPackaging(event.target.value)} placeholder="Tùy chọn: bao bì, quy cách, nhãn riêng..." />
                            </div>
                            <div className="mp-form-group">
                                <label>Ghi chú thêm</label>
                                <textarea rows={3} value={rfqNote} onChange={event => setRfqNote(event.target.value)} placeholder="Thông tin bổ sung cho xưởng" />
                            </div>
                            <div className="mp-modal-actions">
                                <button type="button" className="mp-cancel-btn" onClick={() => setShowOrderModal(false)}>Hủy</button>
                                <button type="submit" className="mp-submit-btn" disabled={submitting}>{submitting ? 'Đang gửi...' : 'Gửi RFQ'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {showPublishModal && (
                <div className="mp-modal-overlay" onClick={() => setShowPublishModal(false)}>
                    <div className="mp-modal" onClick={event => event.stopPropagation()}>
                        <div className="mp-modal-header">
                            <h2>{editingPublishedTeam ? 'Cập nhật hồ sơ xưởng' : 'Đăng xưởng lên thị trường'}</h2>
                            <button className="mp-modal-close" onClick={() => setShowPublishModal(false)}>×</button>
                        </div>
                        <form onSubmit={handlePublish}>
                            <div className="mp-form-group">
                                <label>Chọn xưởng</label>
                                <select
                                    value={publishTeamId}
                                    onChange={event => {
                                        const team = myTeams.find(item => item.id === event.target.value);
                                        if (team) fillPublishForm(team);
                                    }}
                                    required
                                >
                                    {myTeams.map(team => <option key={team.id} value={team.id}>{team.name} {team.isPublished ? '(Đã đăng)' : ''}</option>)}
                                </select>
                            </div>
                            <div className="mp-form-group">
                                <label>Loại hình xưởng *</label>
                                <select value={pubFactoryType} onChange={event => setPubFactoryType(event.target.value)} required>
                                    <option value="">Chọn loại hình</option>
                                    {FACTORY_TYPE_OPTIONS.map(option => <option key={option} value={option}>{option}</option>)}
                                </select>
                            </div>
                            <div className="mp-form-group">
                                <label>Khu vực *</label>
                                <select value={pubRegion} onChange={event => setPubRegion(event.target.value)} required>
                                    <option value="">Chọn khu vực</option>
                                    {REGION_OPTIONS.map(option => <option key={option} value={option}>{option}</option>)}
                                </select>
                            </div>
                            <div className="mp-form-group">
                                <label>Chuyên môn * (giữ Ctrl/Cmd để chọn nhiều)</label>
                                <select
                                    multiple
                                    value={splitMultiValue(pubSpecialty)}
                                    onChange={event => setPubSpecialty(Array.from(event.target.selectedOptions).map(option => option.value).join(', '))}
                                    required
                                >
                                    {SPECIALTY_OPTIONS.map(option => <option key={option} value={option}>{option}</option>)}
                                </select>
                            </div>
                            <div className="mp-form-row">
                                <div className="mp-form-group">
                                    <label>Công suất *</label>
                                    <input type="number" min="1" value={pubCapacityValue} onChange={event => setPubCapacityValue(event.target.value)} placeholder="VD: 2000" required />
                                </div>
                                <div className="mp-form-group">
                                    <label>Đơn vị công suất *</label>
                                    <select value={pubCapacityUnit} onChange={event => setPubCapacityUnit(event.target.value)} required>
                                        <option value="kg/tháng">kg/tháng</option>
                                        <option value="tấn/tháng">tấn/tháng</option>
                                    </select>
                                </div>
                            </div>
                            <div className="mp-form-group">
                                <label>Mô tả năng lực * (tối thiểu 30 ký tự)</label>
                                <textarea rows={4} minLength={30} value={pubDescription} onChange={event => setPubDescription(event.target.value)} required />
                            </div>
                            <div className="mp-form-group">
                                <label>Ảnh xưởng * (1-10 ảnh JPG/PNG/WEBP, tối đa 5MB/ảnh)</label>
                                <input type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={handleFactoryImageFile} />
                                <input value={pubFactoryImageUrl} onChange={event => {
                                    setPubFactoryImageUrl(event.target.value);
                                    setPubFactoryImages(event.target.value ? [event.target.value, ...pubFactoryImages.slice(1)] : pubFactoryImages.slice(1));
                                }} placeholder="Hoặc dán URL ảnh đại diện xưởng" />
                                {pubFactoryImages.length > 0 && (
                                    <div className="mp-factory-image-preview-grid">
                                        {pubFactoryImages.map((image, index) => (
                                            <div className="mp-factory-image-preview" key={`${image}-${index}`}>
                                                <img src={image} alt={`Ảnh xưởng ${index + 1}`} />
                                                <button type="button" onClick={() => {
                                                    const next = pubFactoryImages.filter((_, itemIndex) => itemIndex !== index);
                                                    setPubFactoryImages(next);
                                                    setPubFactoryImageUrl(next[0] || '');
                                                }}>Xóa ảnh</button>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                            <div className="mp-verification-form">
                                <div className="mp-verification-form-head">
                                    <div>
                                        <h3>Gửi thông tin xác minh</h3>
                                        <p>Điền thông tin bên dưới để quản trị viên kiểm tra và duyệt hồ sơ xưởng.</p>
                                    </div>
                                    {selectedPublishTeam && (
                                        <span className={`mp-verification-status ${publishVerificationStatus.toLowerCase()}`}>
                                            {verificationStatusLabel(publishVerificationStatus)}
                                        </span>
                                    )}
                                </div>
                                {selectedPublishTeam?.verificationRejectReason && (
                                    <div className="mp-verification-note">
                                        Lý do từ chối: {selectedPublishTeam.verificationRejectReason}
                                    </div>
                                )}
                                <div className="mp-verification-grid">
                                    <div className="mp-form-group">
                                        <label>Giấy phép kinh doanh * (PDF/JPG/PNG)</label>
                                        <input value={pubBusinessLicense} onChange={event => setPubBusinessLicense(event.target.value)} placeholder="Link tài liệu hoặc tải file bên dưới" required />
                                        <input type="file" accept="application/pdf,image/jpeg,image/png" onChange={event => handleDocumentFile(event, setPubBusinessLicense)} />
                                    </div>
                                    <div className="mp-form-group">
                                        <label>Địa chỉ xưởng *</label>
                                        <input value={pubBusinessAddress} onChange={event => setPubBusinessAddress(event.target.value)} placeholder="Địa chỉ pháp lý / địa chỉ xưởng" required />
                                    </div>
                                    <div className="mp-form-group">
                                        <label>Website doanh nghiệp</label>
                                        <input value={pubWebsiteUrl} onChange={event => setPubWebsiteUrl(event.target.value)} placeholder="https://..." />
                                    </div>
                                    <div className="mp-form-group">
                                        <label>Facebook doanh nghiệp</label>
                                        <input value={pubFacebookUrl} onChange={event => setPubFacebookUrl(event.target.value)} placeholder="https://facebook.com/..." />
                                    </div>
                                    <div className="mp-form-group">
                                        <label>Chứng nhận</label>
                                        <select
                                            multiple
                                            value={pubCertificates}
                                            onChange={event => setPubCertificates(Array.from(event.target.selectedOptions).map(option => option.value))}
                                        >
                                            {CERTIFICATE_OPTIONS.map(option => <option key={option} value={option}>{option}</option>)}
                                        </select>
                                    </div>
                                    <div className="mp-form-group">
                                        <label>File chứng nhận</label>
                                        <input value={pubCertificationDocument} onChange={event => setPubCertificationDocument(event.target.value)} placeholder="Link tài liệu nếu có" />
                                        <input type="file" accept="application/pdf,image/jpeg,image/png" onChange={event => handleDocumentFile(event, setPubCertificationDocument)} />
                                    </div>
                                </div>
                            </div>
                            <div className="mp-modal-actions">
                                <button type="button" className="mp-cancel-btn" onClick={() => setShowPublishModal(false)}>Hủy</button>
                                <button type="submit" className="mp-submit-btn" disabled={publishing}>{publishing ? 'Đang gửi...' : editingPublishedTeam ? 'Lưu và gửi cho quản trị viên' : 'Gửi cho quản trị viên'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

        </div>
    );
}
