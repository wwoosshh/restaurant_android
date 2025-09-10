const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();

// 모델 불러오기
const User = require('../models/User');
const Restaurant = require('../models/Restaurant');

// MongoDB 연결
const connectDB = async () => {
    try {
        await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/restaurant_app');
        console.log('✅ MongoDB 연결 성공: restaurant_app');
    } catch (error) {
        console.error('❌ MongoDB 연결 실패:', error);
        process.exit(1);
    }
};

// 테스트 사용자 데이터
const testUsers = [
    {
        username: '테스트유저1',
        email: 'test@example.com',
        password: 'password123',
        isEmailVerified: true,
        preferences: {
            favoriteCategories: ['한식', '일식'],
            priceRange: '보통'
        }
    },
    {
        username: '맛집탐험가',
        email: 'foodie@example.com',
        password: 'foodie123',
        isEmailVerified: true,
        preferences: {
            favoriteCategories: ['양식', '카페'],
            priceRange: '비싸'
        }
    }
];

// 테스트 맛집 데이터 (서울 기준) - 50+ 개로 확장, mainCategory/subCategory 추가
const testRestaurants = [
    // 한식 카테고리
    {
        name: '명동교자',
        category: '한식', // 기존 호환성
        mainCategory: '한식',
        subCategory: '김치찌개',
        address: '서울특별시 중구 명동2가 25-2',
        location: {
            type: 'Point',
            coordinates: [126.9849, 37.5636] // [경도, 위도]
        },
        phone: '02-776-5348',
        rating: 4.3,
        reviewCount: 2847,
        priceRange: '보통',
        openingHours: '10:30 - 21:30',
        description: '명동에서 유명한 만두 전문점입니다. 육즙이 풍부한 왕만두로 유명해요.',
        menuItems: [
            { name: '왕만두', price: 8000, description: '대표 메뉴', isPopular: true },
            { name: '물만두', price: 7000, description: '깔끔한 맛' },
            { name: '갈비만두', price: 9000, description: '갈비가 들어간 특별한 만두', isPopular: true }
        ],
        tags: ['만두', '전통', '명동', '유명맛집'],
        isVerified: true
    },
    {
        name: '백종원의 본가',
        category: '한식',
        mainCategory: '한식',
        subCategory: '국밥',
        address: '서울특별시 중구 청계천로 40',
        location: {
            type: 'Point',
            coordinates: [126.9895, 37.5665]
        },
        phone: '02-2285-1234',
        rating: 4.2,
        reviewCount: 1893,
        priceRange: '보통',
        openingHours: '11:00 - 22:00',
        description: '백종원 셰프가 직접 운영하는 한식당입니다. 정통 한식의 맛을 느낄 수 있어요.',
        menuItems: [
            { name: '김치찌개', price: 8000, description: '시원한 김치찌개', isPopular: true },
            { name: '된장찌개', price: 7000, description: '구수한 된장찌개' },
            { name: '삼겹살', price: 15000, description: '두툼한 삼겹살', isPopular: true }
        ],
        tags: ['백종원', '한식', '김치찌개', '전통'],
        isVerified: true
    },
    {
        name: '진미평양냉면',
        category: '한식',
        mainCategory: '한식',
        subCategory: '냉면',
        address: '서울특별시 중구 을지로3가 229-1',
        location: {
            type: 'Point',
            coordinates: [126.9915, 37.5640]
        },
        phone: '02-2267-7892',
        rating: 4.4,
        reviewCount: 2156,
        priceRange: '저렴',
        openingHours: '11:30 - 21:00',
        description: '50년 전통의 평양냉면 전문점입니다.',
        menuItems: [
            { name: '평양냉면', price: 9000, description: '시원한 평양냉면', isPopular: true },
            { name: '비빔냉면', price: 9500, description: '매콤한 비빔냉면' },
            { name: '온면', price: 8000, description: '따뜻한 국수' }
        ],
        tags: ['냉면', '평양냉면', '전통', '을지로'],
        isVerified: true
    },
    // 구이 카테고리 (곱창전골목포집 올바른 분류)
    {
        name: '곱창전골 목포집',
        category: '구이', // 수정된 카테고리
        mainCategory: '구이',
        subCategory: '곱창',
        address: '서울특별시 강남구 신사동 549-7',
        location: {
            type: 'Point',
            coordinates: [127.0196, 37.5217]
        },
        phone: '02-544-4958',
        rating: 4.5,
        reviewCount: 1523,
        priceRange: '비싸',
        openingHours: '17:00 - 02:00',
        description: '신사동에 위치한 곱창전골 맛집입니다. 신선한 곱창과 진한 국물이 일품이에요.',
        menuItems: [
            { name: '곱창전골', price: 15000, description: '2인분 기준', isPopular: true },
            { name: '막창구이', price: 12000, description: '바삭하게 구운 막창' },
            { name: '소주', price: 4000, description: '곱창과 찰떡궁합' }
        ],
        tags: ['곱창', '전골', '신사동', '술집'],
        isVerified: true
    },
    {
        name: '삼원가든',
        category: '구이',
        mainCategory: '구이',
        subCategory: '갈비',
        address: '서울특별시 강남구 신사동 535-3',
        location: {
            type: 'Point',
            coordinates: [127.0201, 37.5225]
        },
        phone: '02-548-3030',
        rating: 4.6,
        reviewCount: 2847,
        priceRange: '고급',
        openingHours: '17:00 - 24:00',
        description: '신사동 고급 갈비집입니다. 최고급 한우를 사용합니다.',
        menuItems: [
            { name: '한우갈비', price: 45000, description: '최고급 한우', isPopular: true },
            { name: 'LA갈비', price: 38000, description: '부드러운 LA갈비' },
            { name: '갈비탕', price: 15000, description: '진한 갈비탕' }
        ],
        tags: ['갈비', '한우', '고급', '신사동'],
        isVerified: true
    },
    // 중식 카테고리  
    {
        name: '홍콩반점0410',
        category: '중식',
        mainCategory: '중식',
        subCategory: '짜장면',
        address: '서울특별시 서초구 서초동 1303-34',
        location: {
            type: 'Point',
            coordinates: [127.0276, 37.4937]
        },
        phone: '02-522-0410',
        rating: 4.2,
        reviewCount: 3241,
        priceRange: '보통',
        openingHours: '11:00 - 22:00',
        description: '서초동 대표 중식당입니다. 짜장면과 탕수육이 특히 맛있어요.',
        menuItems: [
            { name: '짜장면', price: 6000, description: '고전 중식의 정석', isPopular: true },
            { name: '탕수육', price: 18000, description: '바삭한 식감', isPopular: true },
            { name: '짬뽕', price: 7000, description: '얼큰한 국물' }
        ],
        tags: ['짜장면', '탕수육', '중식', '배달'],
        isVerified: true
    },
    {
        name: '마라롱샤',
        category: '중식',
        mainCategory: '중식',
        subCategory: '마라탕',
        address: '서울특별시 강남구 역삼동 647-23',
        location: {
            type: 'Point',
            coordinates: [127.0335, 37.4995]
        },
        phone: '02-567-8901',
        rating: 4.3,
        reviewCount: 1892,
        priceRange: '보통',
        openingHours: '11:00 - 23:00',
        description: '정통 사천식 마라탕 전문점입니다. 얼큰하고 시원한 맛이 일품이에요.',
        menuItems: [
            { name: '마라탕', price: 12000, description: '매운 사천식 탕', isPopular: true },
            { name: '마라샹궈', price: 15000, description: '볶음 마라요리' },
            { name: '꿔바로우', price: 18000, description: '중국식 탕수육', isPopular: true }
        ],
        tags: ['마라탕', '사천요리', '매운맛', '역삼'],
        isVerified: true
    },
    {
        name: '양꼬치 대가',
        category: '중식',
        mainCategory: '중식',
        subCategory: '양꼬치',
        address: '서울특별시 마포구 홍익로5길 20',
        location: {
            type: 'Point',
            coordinates: [126.9248, 37.5512]
        },
        phone: '02-325-7890',
        rating: 4.4,
        reviewCount: 2567,
        priceRange: '보통',
        openingHours: '17:00 - 02:00',
        description: '신선한 양고기로 만든 양꼬치 전문점입니다.',
        menuItems: [
            { name: '양꼬치', price: 2500, description: '1개 기준, 신선한 양고기', isPopular: true },
            { name: '양갈비', price: 25000, description: '부드러운 양갈비' },
            { name: '칭다오맥주', price: 4000, description: '양꼬치와 찰떡궁합', isPopular: true }
        ],
        tags: ['양꼬치', '양고기', '홍대', '술집'],
        isVerified: true
    },
    // 일식 카테고리
    {
        name: '스시조',
        category: '일식',
        mainCategory: '일식',
        subCategory: null,
        address: '서울특별시 강남구 청담동 129-1',
        location: {
            type: 'Point',
            coordinates: [127.0471, 37.5206]
        },
        phone: '02-545-2440',
        rating: 4.7,
        reviewCount: 892,
        priceRange: '고급',
        openingHours: '12:00 - 22:00',
        description: '청담동 프리미엄 스시 오마카세 전문점입니다. 최고급 재료만을 사용합니다.',
        menuItems: [
            { name: '오마카세 A', price: 80000, description: '10피스 코스', isPopular: true },
            { name: '오마카세 B', price: 120000, description: '15피스 코스', isPopular: true },
            { name: '사시미 모둠', price: 45000, description: '신선한 회 모둠' }
        ],
        tags: ['스시', '오마카세', '고급', '청담동'],
        isVerified: true
    },
    {
        name: '츠키지 스시잔',
        category: '일식',
        mainCategory: '일식',
        subCategory: null,
        address: '서울특별시 서초구 반포대로 222',
        location: {
            type: 'Point',
            coordinates: [127.0145, 37.4985]
        },
        phone: '02-535-1234',
        rating: 4.5,
        reviewCount: 1245,
        priceRange: '비싸',
        openingHours: '18:00 - 23:00',
        description: '일본 도쿄 츠키지에서 온 정통 일식당입니다.',
        menuItems: [
            { name: '스시 세트', price: 45000, description: '신선한 스시 모음', isPopular: true },
            { name: '사시미 정식', price: 38000, description: '최고급 사시미' },
            { name: '우동 정식', price: 15000, description: '진한 국물의 우동' }
        ],
        tags: ['스시', '사시미', '츠키지', '정통일식'],
        isVerified: true
    },
    // 양식 카테고리  
    {
        name: '라 테이블 드 조엘 로부숑',
        category: '양식',
        mainCategory: '양식',
        subCategory: null,
        address: '서울특별시 강남구 청담동 118-1',
        location: {
            type: 'Point',
            coordinates: [127.0445, 37.5197]
        },
        phone: '02-3442-9736',
        rating: 4.8,
        reviewCount: 567,
        priceRange: '고급',
        openingHours: '12:00 - 15:00, 18:00 - 22:00',
        description: '미슐랭 3스타 셰프 조엘 로부숑의 프렌치 레스토랑입니다.',
        menuItems: [
            { name: '디너 코스', price: 180000, description: '7코스 프렌치 정찬', isPopular: true },
            { name: '런치 코스', price: 98000, description: '4코스 런치' },
            { name: '와인 페어링', price: 85000, description: '코스와 함께하는 와인' }
        ],
        tags: ['프렌치', '미슐랭', '고급', '파인다이닝'],
        isVerified: true
    },
    {
        name: '더 플레이트',
        category: '양식',
        mainCategory: '양식',
        subCategory: null,
        address: '서울특별시 용산구 이태원로 195',
        location: {
            type: 'Point',
            coordinates: [126.9912, 37.5344]
        },
        phone: '02-797-4567',
        rating: 4.3,
        reviewCount: 1834,
        priceRange: '비싸',
        openingHours: '17:30 - 22:30',
        description: '이태원의 모던 양식 레스토랑입니다. 창의적인 요리를 선보입니다.',
        menuItems: [
            { name: '스테이크 세트', price: 45000, description: '프리미엄 스테이크', isPopular: true },
            { name: '파스타 코스', price: 28000, description: '수제 파스타' },
            { name: '시저샐러드', price: 18000, description: '신선한 야채', isPopular: true }
        ],
        tags: ['양식', '스테이크', '이태원', '모던'],
        isVerified: true
    },
    // 커피/차 카테고리
    {
        name: '블루보틀 커피 삼청점',
        category: '커피/차',
        mainCategory: '커피/차',
        subCategory: null,
        address: '서울특별시 종로구 삼청동 83',
        location: {
            type: 'Point',
            coordinates: [126.9819, 37.5834]
        },
        phone: '02-3210-0159',
        rating: 4.1,
        reviewCount: 1847,
        priceRange: '비싸',
        openingHours: '08:00 - 21:00',
        description: '캘리포니아에서 온 스페셜티 커피 전문점입니다. 삼청동의 아름다운 풍경과 함께 즐기세요.',
        menuItems: [
            { name: '드립 커피', price: 6500, description: '핸드드립 원두커피', isPopular: true },
            { name: '아메리카노', price: 5500, description: '에스프레소 베이스' },
            { name: '라떼', price: 6000, description: '부드러운 스팀밀크', isPopular: true }
        ],
        tags: ['스페셜티커피', '핸드드립', '삼청동', '브런치'],
        isVerified: true
    },
    {
        name: '스타벅스 강남역점',
        category: '커피/차',
        mainCategory: '커피/차',
        subCategory: null,
        address: '서울특별시 강남구 역삼동 814-6',
        location: {
            type: 'Point',
            coordinates: [127.0276, 37.4979]
        },
        phone: '02-508-1004',
        rating: 4.0,
        reviewCount: 5623,
        priceRange: '보통',
        openingHours: '06:00 - 24:00',
        description: '강남역 근처의 대표 커피 체인점입니다.',
        menuItems: [
            { name: '아메리카노', price: 4100, description: '클래식 아메리카노', isPopular: true },
            { name: '카라멜 마키아토', price: 5900, description: '달콤한 카라멜향' },
            { name: '프라푸치노', price: 6400, description: '시원한 얼음음료', isPopular: true }
        ],
        tags: ['커피', '체인점', '강남역', '회의'],
        isVerified: true
    },
    // 디저트 카테고리
    {
        name: '설빙 강남점',
        category: '디저트',
        mainCategory: '디저트',
        subCategory: '빙수',
        address: '서울특별시 강남구 역삼동 834-3',
        location: {
            type: 'Point',
            coordinates: [127.0286, 37.5008]
        },
        phone: '02-567-1004',
        rating: 4.0,
        reviewCount: 2156,
        priceRange: '보통',
        openingHours: '11:00 - 23:00',
        description: '한국식 빙수 전문점입니다. 시원한 빙수와 다양한 토핑을 즐기세요.',
        menuItems: [
            { name: '인절미 설빙', price: 9900, description: '대표 메뉴', isPopular: true },
            { name: '망고 치즈 설빙', price: 12900, description: '망고와 치즈의 조합' },
            { name: '초콜릿 설빙', price: 10900, description: '진한 초콜릿 맛', isPopular: true }
        ],
        tags: ['빙수', '디저트', '강남', '여름간식'],
        isVerified: true
    },
    {
        name: '배스킨라빈스 홍대점',
        category: '디저트',
        mainCategory: '디저트',
        subCategory: '아이스크림',
        address: '서울특별시 마포구 홍익로 63',
        location: {
            type: 'Point',
            coordinates: [126.9212, 37.5563]
        },
        phone: '02-332-3131',
        rating: 4.2,
        reviewCount: 1674,
        priceRange: '보통',
        openingHours: '10:00 - 23:00',
        description: '31가지 맛 아이스크림 전문점입니다.',
        menuItems: [
            { name: '싱글 레귤러', price: 3200, description: '1스쿱 아이스크림', isPopular: true },
            { name: '더블 레귤러', price: 5800, description: '2스쿱 아이스크림' },
            { name: '아이스크림 케이크', price: 25000, description: '생일케이크', isPopular: true }
        ],
        tags: ['아이스크림', '31가지맛', '홍대', '케이크'],
        isVerified: true
    },
    // 치킨 카테고리
    {
        name: 'BBQ 치킨 강남점',
        category: '치킨',
        mainCategory: '치킨',
        subCategory: 'BBQ',
        address: '서울특별시 강남구 역삼동 789-45',
        location: {
            type: 'Point',
            coordinates: [127.0286, 37.4979]
        },
        phone: '02-567-8888',
        rating: 4.4,
        reviewCount: 3847,
        priceRange: '보통',
        openingHours: '16:00 - 02:00',
        description: '바삭한 황금올리브 치킨으로 유명한 프라이드 치킨 전문점입니다.',
        menuItems: [
            { name: '황금올리브 치킨', price: 18000, description: '대표 메뉴', isPopular: true },
            { name: '양념치킨', price: 19000, description: '달콤매콤한 양념' },
            { name: '반반치킨', price: 19500, description: '후라이드+양념 반반', isPopular: true }
        ],
        tags: ['치킨', '후라이드', '양념치킨', '배달'],
        isVerified: true
    },
    {
        name: '교촌치킨 신촌점',
        category: '치킨',
        mainCategory: '치킨',
        subCategory: '교촌',
        address: '서울특별시 서대문구 창천동 31-44',
        location: {
            type: 'Point',
            coordinates: [126.9366, 37.5595]
        },
        phone: '02-313-7777',
        rating: 4.5,
        reviewCount: 2834,
        priceRange: '보통',
        openingHours: '16:00 - 02:00',
        description: '허니콤보로 유명한 교촌치킨입니다.',
        menuItems: [
            { name: '허니콤보', price: 19000, description: '교촌 대표메뉴', isPopular: true },
            { name: '레드콤보', price: 19000, description: '매콤한 맛' },
            { name: '오리지널', price: 17000, description: '담백한 맛', isPopular: true }
        ],
        tags: ['치킨', '허니콤보', '교촌', '신촌'],
        isVerified: true
    },
    {
        name: 'BHC치킨 홍대점',
        category: '치킨',
        mainCategory: '치킨',
        subCategory: 'BHC',
        address: '서울특별시 마포구 홍익로 63',
        location: {
            type: 'Point',
            coordinates: [126.9212, 37.5563]
        },
        phone: '02-322-4567',
        rating: 4.3,
        reviewCount: 2573,
        priceRange: '보통',
        openingHours: '16:00 - 03:00',
        description: '뿌링클로 유명한 BHC 치킨입니다.',
        menuItems: [
            { name: '뿌링클', price: 18000, description: '대표 메뉴', isPopular: true },
            { name: '맛초킹', price: 19000, description: '달콤매콤한 맛' },
            { name: '후라이드', price: 16000, description: '바삭한 후라이드' }
        ],
        tags: ['치킨', '뿌링클', '홍대', 'BHC'],
        isVerified: true
    },
    // 피자 카테고리
    {
        name: '도미노피자 신촌점',
        category: '피자',
        mainCategory: '피자',
        subCategory: '도미노',
        address: '서울특별시 서대문구 창천동 52-5',
        location: {
            type: 'Point',
            coordinates: [126.9366, 37.5595]
        },
        phone: '02-313-7979',
        rating: 4.1,
        reviewCount: 4251,
        priceRange: '보통',
        openingHours: '11:00 - 24:00',
        description: '미국식 피자 전문점으로 다양한 토핑을 선택할 수 있습니다.',
        menuItems: [
            { name: '페퍼로니 피자', price: 24000, description: '클래식 페퍼로니', isPopular: true },
            { name: '불고기 피자', price: 26000, description: '한국식 불고기 토핑' },
            { name: '콤비네이션 피자', price: 28000, description: '다양한 토핑', isPopular: true }
        ],
        tags: ['피자', '미국식', '배달', '신촌'],
        isVerified: true
    },
    {
        name: '피자헛 이태원점',
        category: '피자',
        mainCategory: '피자',
        subCategory: '피자헛',
        address: '서울특별시 용산구 이태원동 119-25',
        location: {
            type: 'Point',
            coordinates: [126.9947, 37.5339]
        },
        phone: '02-790-7979',
        rating: 4.0,
        reviewCount: 3156,
        priceRange: '보통',
        openingHours: '11:00 - 23:00',
        description: '두꺼운 도우로 유명한 피자 전문점입니다.',
        menuItems: [
            { name: '슈퍼슈프림 피자', price: 29000, description: '대표 메뉴', isPopular: true },
            { name: '치즈크러스트 피자', price: 32000, description: '치즈 가득한 크러스트' },
            { name: '웨지감자', price: 8000, description: '바삭한 감자' }
        ],
        tags: ['피자', '치즈크러스트', '이태원', '파티'],
        isVerified: true
    },
    {
        name: '청년피자 건대점',
        category: '피자',
        mainCategory: '피자',
        subCategory: '청년피자',
        address: '서울특별시 광진구 능동 227-45',
        location: {
            type: 'Point',
            coordinates: [127.0695, 37.5412]
        },
        phone: '02-456-7890',
        rating: 4.3,
        reviewCount: 1897,
        priceRange: '저렴',
        openingHours: '17:00 - 03:00',
        description: '가성비 좋은 청년 피자 전문점입니다.',
        menuItems: [
            { name: '고르곤졸라 피자', price: 19000, description: '치즈 듬뿍', isPopular: true },
            { name: '페페로니 피자', price: 16000, description: '클래식한 맛' },
            { name: '치킨바베큐 피자', price: 18000, description: '달콤바베큐', isPopular: true }
        ],
        tags: ['피자', '가성비', '청년피자', '건대'],
        isVerified: true
    },
    // 버거 카테고리
    {
        name: '쉑쉑버거 강남점',
        category: '버거',
        mainCategory: '버거',
        subCategory: '쉐이크쉑',
        address: '서울특별시 강남구 테헤란로 152',
        location: {
            type: 'Point',
            coordinates: [127.0297, 37.5048]
        },
        phone: '02-6203-7800',
        rating: 4.6,
        reviewCount: 5847,
        priceRange: '비싸',
        openingHours: '11:00 - 22:00',
        description: '뉴욕에서 온 프리미엄 버거 전문점입니다.',
        menuItems: [
            { name: '쉑버거', price: 8900, description: '대표 메뉴', isPopular: true },
            { name: '치즈버거', price: 7900, description: '치즈가 들어간 버거' },
            { name: '쉑프라이', price: 4500, description: '바삭한 감자튀김', isPopular: true }
        ],
        tags: ['버거', '프리미엄', '뉴욕스타일', '강남'],
        isVerified: true
    },
    {
        name: '버거킹 홍대점',
        category: '버거',
        mainCategory: '버거',
        subCategory: '버거킹',
        address: '서울특별시 마포구 양화로 188',
        location: {
            type: 'Point',
            coordinates: [126.9205, 37.5486]
        },
        phone: '02-333-3456',
        rating: 3.9,
        reviewCount: 3421,
        priceRange: '저렴',
        openingHours: '08:00 - 24:00',
        description: '불 맛이 나는 와퍼로 유명한 패스트푸드점입니다.',
        menuItems: [
            { name: '와퍼 세트', price: 7900, description: '대표 메뉴', isPopular: true },
            { name: '치킨버거 세트', price: 6900, description: '바삭한 치킨패티' },
            { name: '너겟킹', price: 3500, description: '치킨 너겟' }
        ],
        tags: ['버거', '와퍼', '불맛', '홍대'],
        isVerified: true
    },
    {
        name: '맥도날드 명동점',
        category: '버거',
        mainCategory: '버거',
        subCategory: '맥도날드',
        address: '서울특별시 중구 명동2가 31-1',
        location: {
            type: 'Point',
            coordinates: [126.9851, 37.5645]
        },
        phone: '02-771-0063',
        rating: 3.8,
        reviewCount: 4523,
        priceRange: '저렴',
        openingHours: '24시간',
        description: '24시간 운영하는 패스트푸드점입니다. 빠르고 간편한 식사를 원할 때 좋아요.',
        menuItems: [
            { name: '빅맥 세트', price: 6900, description: '대표 버거 세트', isPopular: true },
            { name: '치킨맥너겟', price: 4500, description: '바삭한 치킨' },
            { name: '감자튀김', price: 2300, description: '바삭한 감자튀김', isPopular: true }
        ],
        tags: ['패스트푸드', '버거', '24시간', '명동'],
        isVerified: true
    },
    // 도시락 카테고리
    {
        name: '본도시락 역삼점',
        category: '도시락',
        mainCategory: '도시락',
        subCategory: null,
        address: '서울특별시 강남구 역삼동 825-23',
        location: {
            type: 'Point',
            coordinates: [127.0356, 37.5002]
        },
        phone: '02-567-2345',
        rating: 4.0,
        reviewCount: 2156,
        priceRange: '저렴',
        openingHours: '06:00 - 22:00',
        description: '한국식 도시락 전문점으로 든든한 한 끼를 제공합니다.',
        menuItems: [
            { name: '제육도시락', price: 5500, description: '매콤한 제육볶음', isPopular: true },
            { name: '치킨도시락', price: 6000, description: '바삭한 치킨' },
            { name: '불고기도시락', price: 6500, description: '달콤한 불고기', isPopular: true }
        ],
        tags: ['도시락', '한식', '든든', '사무실'],
        isVerified: true
    },
    {
        name: '더반찬 신림점',
        category: '도시락',
        mainCategory: '도시락',
        subCategory: null,
        address: '서울특별시 관악구 신림동 1422-13',
        location: {
            type: 'Point',
            coordinates: [126.9295, 37.4850]
        },
        phone: '02-875-3456',
        rating: 4.1,
        reviewCount: 1534,
        priceRange: '저렴',
        openingHours: '07:00 - 21:00',
        description: '집밥 스타일 도시락 전문점입니다.',
        menuItems: [
            { name: '집밥도시락', price: 5000, description: '집에서 만든 것 같은 맛', isPopular: true },
            { name: '김치찌개도시락', price: 5500, description: '김치찌개와 밥' },
            { name: '된장찌개도시락', price: 5000, description: '구수한 된장찌개', isPopular: true }
        ],
        tags: ['도시락', '집밥', '반찬', '신림'],
        isVerified: true
    },
    // 분식 카테고리
    {
        name: '떡볶이 신당동',
        category: '분식',
        mainCategory: '분식',
        subCategory: '떡볶이',
        address: '서울특별시 중구 신당동 370-5',
        location: {
            type: 'Point',
            coordinates: [127.0086, 37.5609]
        },
        phone: '02-2233-4567',
        rating: 4.3,
        reviewCount: 1847,
        priceRange: '저렴',
        openingHours: '10:00 - 22:00',
        description: '신당동 떡볶이 골목의 유명한 떡볶이 전문점입니다.',
        menuItems: [
            { name: '떡볶이', price: 3000, description: '매콤달콤한 떡볶이', isPopular: true },
            { name: '순대', price: 4000, description: '쫄깃한 순대' },
            { name: '김밥', price: 2500, description: '속이 꽉찬 김밥' }
        ],
        tags: ['떡볶이', '분식', '신당동', '길거리음식'],
        isVerified: true
    },
    {
        name: '김밥천국 신림점',
        category: '분식',
        mainCategory: '분식',
        subCategory: '김밥',
        address: '서울특별시 관악구 신림동 1422-5',
        location: {
            type: 'Point',
            coordinates: [126.9292, 37.4844]
        },
        phone: '02-875-6789',
        rating: 3.8,
        reviewCount: 2973,
        priceRange: '저렴',
        openingHours: '24시간',
        description: '24시간 운영하는 분식점으로 다양한 김밥과 분식을 즐길 수 있습니다.',
        menuItems: [
            { name: '참치김밥', price: 3500, description: '참치가 들어간 김밥', isPopular: true },
            { name: '라면', price: 3000, description: '얼큰한 라면' },
            { name: '돈까스', price: 6000, description: '바삭한 돈까스', isPopular: true }
        ],
        tags: ['김밥', '분식', '24시간', '신림'],
        isVerified: true
    },
    {
        name: '신전떡볶이 홍대점',
        category: '분식',
        mainCategory: '분식',
        subCategory: '신전떡볶이',
        address: '서울특별시 마포구 홍익로3길 17',
        location: {
            type: 'Point',
            coordinates: [126.9243, 37.5534]
        },
        phone: '02-325-1234',
        rating: 4.2,
        reviewCount: 2435,
        priceRange: '저렴',
        openingHours: '11:00 - 23:00',
        description: '프랜차이즈 떡볶이 전문점입니다.',
        menuItems: [
            { name: '신전떡볶이', price: 4000, description: '매운 떡볶이', isPopular: true },
            { name: '로제떡볶이', price: 5000, description: '크림소스 떡볶이' },
            { name: '튀김', price: 500, description: '각종 튀김', isPopular: true }
        ],
        tags: ['떡볶이', '신전', '홍대', '매운맛'],
        isVerified: true
    },
    // 돈까스 카테고리
    {
        name: '코코이찌방야 명동점',
        category: '돈까스',
        mainCategory: '돈까스',
        subCategory: null,
        address: '서울특별시 중구 명동1가 59-4',
        location: {
            type: 'Point',
            coordinates: [126.9857, 37.5650]
        },
        phone: '02-318-2000',
        rating: 4.2,
        reviewCount: 3524,
        priceRange: '보통',
        openingHours: '11:00 - 22:00',
        description: '일본식 돈까스 카레 전문점입니다.',
        menuItems: [
            { name: '포크카레', price: 9000, description: '돈까스와 카레의 조합', isPopular: true },
            { name: '치킨카레', price: 8500, description: '치킨까스와 카레' },
            { name: '규카츠', price: 11000, description: '소고기 돈까스', isPopular: true }
        ],
        tags: ['돈까스', '카레', '일식', '명동'],
        isVerified: true
    },
    {
        name: '돈까스클럽 신촌점',
        category: '돈까스',
        mainCategory: '돈까스',
        subCategory: null,
        address: '서울특별시 서대문구 창천동 52-12',
        location: {
            type: 'Point',
            coordinates: [126.9375, 37.5601]
        },
        phone: '02-313-5678',
        rating: 4.3,
        reviewCount: 1834,
        priceRange: '보통',
        openingHours: '11:00 - 21:30',
        description: '바삭한 수제 돈까스 전문점입니다.',
        menuItems: [
            { name: '등심돈까스', price: 8000, description: '두툼한 등심', isPopular: true },
            { name: '치즈돈까스', price: 9500, description: '치즈가 들어간 돈까스' },
            { name: '생선까스', price: 9000, description: '담백한 생선까스', isPopular: true }
        ],
        tags: ['돈까스', '수제', '신촌', '바삭'],
        isVerified: true
    },
    // 족발/보쌈 카테고리
    {
        name: '장수족발 홍대점',
        category: '족발/보쌈',
        mainCategory: '족발/보쌈',
        subCategory: null,
        address: '서울특별시 마포구 홍익로3길 11',
        location: {
            type: 'Point',
            coordinates: [126.9224, 37.5528]
        },
        phone: '02-322-7788',
        rating: 4.5,
        reviewCount: 2156,
        priceRange: '보통',
        openingHours: '17:00 - 03:00',
        description: '쫄깃하고 부드러운 족발과 보쌈을 전문으로 하는 맛집입니다.',
        menuItems: [
            { name: '족발', price: 22000, description: '대표 메뉴 (중)', isPopular: true },
            { name: '보쌈', price: 25000, description: '부드러운 보쌈고기 (중)' },
            { name: '족발보쌈 세트', price: 35000, description: '족발+보쌈 세트', isPopular: true }
        ],
        tags: ['족발', '보쌈', '술안주', '홍대'],
        isVerified: true
    },
    {
        name: '원할머니보쌈 강남점',
        category: '족발/보쌈',
        mainCategory: '족발/보쌈',
        subCategory: null,
        address: '서울특별시 강남구 역삼동 814-15',
        location: {
            type: 'Point',
            coordinates: [127.0285, 37.4985]
        },
        phone: '02-508-9876',
        rating: 4.2,
        reviewCount: 2845,
        priceRange: '보통',
        openingHours: '16:00 - 02:00',
        description: '프랜차이즈 보쌈족발 전문점입니다.',
        menuItems: [
            { name: '보쌈정식', price: 28000, description: '보쌈 + 김치 + 쌈채소', isPopular: true },
            { name: '족발정식', price: 26000, description: '족발 + 김치 + 쌈채소' },
            { name: '막국수', price: 6000, description: '시원한 막국수', isPopular: true }
        ],
        tags: ['보쌈', '족발', '강남', '정식'],
        isVerified: true
    },
    // 찜/탕 카테고리
    {
        name: '본죽 & 비빔밥 카페 강남점',
        category: '죽',
        mainCategory: '죽',
        subCategory: null,
        address: '서울특별시 강남구 테헤란로 238',
        location: {
            type: 'Point',
            coordinates: [127.0298, 37.5042]
        },
        phone: '02-508-1234',
        rating: 4.1,
        reviewCount: 1543,
        priceRange: '보통',
        openingHours: '07:00 - 22:00',
        description: '건강한 죽과 비빔밥을 전문으로 하는 한식당입니다.',
        menuItems: [
            { name: '전복죽', price: 12000, description: '고급 전복이 들어간 죽', isPopular: true },
            { name: '닭죽', price: 8000, description: '담백한 닭죽' },
            { name: '비빔밥', price: 9000, description: '각종 나물이 들어간 비빔밥', isPopular: true }
        ],
        tags: ['죽', '비빔밥', '건강식', '강남'],
        isVerified: true
    },
    {
        name: '옛날손만두 홍대점',
        category: '찜/탕',
        mainCategory: '찜/탕',
        subCategory: '갈비탕',
        address: '서울특별시 마포구 홍익로 25',
        location: {
            type: 'Point',
            coordinates: [126.9235, 37.5545]
        },
        phone: '02-324-5678',
        rating: 4.4,
        reviewCount: 1976,
        priceRange: '보통',
        openingHours: '11:00 - 22:00',
        description: '손으로 빚은 수제만두와 따뜻한 탕류 전문점입니다.',
        menuItems: [
            { name: '만두국', price: 7000, description: '손만두가 들어간 국', isPopular: true },
            { name: '떡만두국', price: 8000, description: '떡과 만두가 함께' },
            { name: '갈비탕', price: 12000, description: '진한 갈비탕', isPopular: true }
        ],
        tags: ['만두', '탕', '수제', '홍대'],
        isVerified: true
    },
    // 새로운 카테고리들 - 더 많은 맛집들
    // 회/해물 카테고리
    {
        name: '노량진 수산시장 횟집',
        category: '회/해물',
        mainCategory: '회/해물',
        subCategory: null,
        address: '서울특별시 동작구 노량진동 13-8',
        location: {
            type: 'Point',
            coordinates: [126.9425, 37.5145]
        },
        phone: '02-814-1234',
        rating: 4.6,
        reviewCount: 3245,
        priceRange: '비싸',
        openingHours: '06:00 - 22:00',
        description: '노량진 수산시장의 신선한 회를 즐길 수 있는 횟집입니다.',
        menuItems: [
            { name: '모듬회', price: 35000, description: '싱싱한 모듬회', isPopular: true },
            { name: '광어회', price: 25000, description: '쫄깃한 광어회' },
            { name: '매운탕', price: 8000, description: '얼큰한 매운탕', isPopular: true }
        ],
        tags: ['회', '해물', '노량진', '수산시장'],
        isVerified: true
    },
    {
        name: '해물찜 대게나라',
        category: '회/해물',
        mainCategory: '회/해물',
        subCategory: null,
        address: '서울특별시 서초구 서초동 1303-28',
        location: {
            type: 'Point',
            coordinates: [127.0289, 37.4945]
        },
        phone: '02-522-7777',
        rating: 4.3,
        reviewCount: 2156,
        priceRange: '비싸',
        openingHours: '17:00 - 24:00',
        description: '신선한 대게와 해물찜 전문점입니다.',
        menuItems: [
            { name: '대게', price: 80000, description: '1kg 기준 대게', isPopular: true },
            { name: '해물찜', price: 35000, description: '각종 해물이 들어간 찜' },
            { name: '킹크랩', price: 120000, description: '프리미엄 킹크랩', isPopular: true }
        ],
        tags: ['대게', '해물찜', '서초', '고급'],
        isVerified: true
    },
    // 간식 카테고리
    {
        name: '호떡집 명동',
        category: '간식',
        mainCategory: '간식',
        subCategory: null,
        address: '서울특별시 중구 명동2가 54-3',
        location: {
            type: 'Point',
            coordinates: [126.9845, 37.5632]
        },
        phone: '02-776-8888',
        rating: 4.0,
        reviewCount: 1823,
        priceRange: '저렴',
        openingHours: '10:00 - 22:00',
        description: '명동의 유명한 호떡 전문점입니다.',
        menuItems: [
            { name: '씨앗호떡', price: 1500, description: '견과류가 들어간 호떡', isPopular: true },
            { name: '설탕호떡', price: 1000, description: '달콤한 설탕호떡' },
            { name: '치즈호떡', price: 2000, description: '치즈가 들어간 호떡', isPopular: true }
        ],
        tags: ['호떡', '간식', '명동', '길거리음식'],
        isVerified: true
    },
    {
        name: '붕어빵 할머니',
        category: '간식',
        mainCategory: '간식',
        subCategory: null,
        address: '서울특별시 종로구 종로3가 159',
        location: {
            type: 'Point',
            coordinates: [126.9913, 37.5708]
        },
        phone: '02-2265-4321',
        rating: 4.2,
        reviewCount: 1456,
        priceRange: '저렴',
        openingHours: '15:00 - 23:00',
        description: '겨울철 대표 간식 붕어빵 전문점입니다.',
        menuItems: [
            { name: '팥붕어빵', price: 800, description: '달콤한 팥이 들어간 붕어빵', isPopular: true },
            { name: '슈크림붕어빵', price: 1000, description: '부드러운 슈크림' },
            { name: '피자붕어빵', price: 1200, description: '새로운 맛의 피자맛', isPopular: true }
        ],
        tags: ['붕어빵', '간식', '종로', '겨울간식'],
        isVerified: true
    },
    // 아시안 카테고리
    {
        name: '타이식당 방콕',
        category: '아시안',
        mainCategory: '아시안',
        subCategory: null,
        address: '서울특별시 용산구 이태원로 203',
        location: {
            type: 'Point',
            coordinates: [126.9925, 37.5352]
        },
        phone: '02-797-8901',
        rating: 4.4,
        reviewCount: 2134,
        priceRange: '보통',
        openingHours: '11:30 - 22:00',
        description: '정통 태국음식을 맛볼 수 있는 타이 레스토랑입니다.',
        menuItems: [
            { name: '팟타이', price: 12000, description: '태국식 볶음면', isPopular: true },
            { name: '똠양꿍', price: 15000, description: '새콤매콤한 태국 전통 수프' },
            { name: '그린커리', price: 16000, description: '코코넛 밀크 커리', isPopular: true }
        ],
        tags: ['타이음식', '태국음식', '이태원', '아시안'],
        isVerified: true
    },
    {
        name: '베트남 쌀국수 하노이',
        category: '아시안',
        mainCategory: '아시안',
        subCategory: null,
        address: '서울특별시 마포구 합정동 369-24',
        location: {
            type: 'Point',
            coordinates: [126.9134, 37.5495]
        },
        phone: '02-332-5555',
        rating: 4.2,
        reviewCount: 1876,
        priceRange: '저렴',
        openingHours: '10:00 - 21:00',
        description: '정통 베트남 쌀국수 전문점입니다.',
        menuItems: [
            { name: '쌀국수', price: 8000, description: '정통 베트남 쌀국수', isPopular: true },
            { name: '분짜', price: 10000, description: '베트남 쌀국수 샐러드' },
            { name: '월남쌈', price: 12000, description: '신선한 베트남 월남쌈', isPopular: true }
        ],
        tags: ['베트남음식', '쌀국수', '합정', '아시안'],
        isVerified: true
    },
    // 샌드위치 카테고리
    {
        name: '써브웨이 강남점',
        category: '샌드위치',
        mainCategory: '샌드위치',
        subCategory: null,
        address: '서울특별시 강남구 역삼동 825-18',
        location: {
            type: 'Point',
            coordinates: [127.0345, 37.4998]
        },
        phone: '02-567-1111',
        rating: 4.1,
        reviewCount: 3456,
        priceRange: '보통',
        openingHours: '08:00 - 22:00',
        description: '신선한 재료로 만드는 샌드위치 전문점입니다.',
        menuItems: [
            { name: 'B.M.T', price: 7900, description: '베이컨, 햄, 살라미', isPopular: true },
            { name: '치킨 테리야끼', price: 8400, description: '달콤한 테리야끼 소스' },
            { name: '참치', price: 6900, description: '참치 샌드위치', isPopular: true }
        ],
        tags: ['샌드위치', '써브웨이', '강남', '헬시'],
        isVerified: true
    },
    {
        name: '이삭토스트 신촌점',
        category: '샌드위치',
        mainCategory: '샌드위치',
        subCategory: null,
        address: '서울특별시 서대문구 신촌로 83',
        location: {
            type: 'Point',
            coordinates: [126.9356, 37.5586]
        },
        phone: '02-313-2222',
        rating: 3.9,
        reviewCount: 2789,
        priceRange: '저렴',
        openingHours: '06:30 - 22:00',
        description: '한국식 토스트 전문점입니다.',
        menuItems: [
            { name: '햄치즈토스트', price: 3500, description: '클래식 토스트', isPopular: true },
            { name: '베이컨토스트', price: 4000, description: '바삭한 베이컨' },
            { name: '딸기잼토스트', price: 2500, description: '달콤한 딸기잼', isPopular: true }
        ],
        tags: ['토스트', '이삭토스트', '신촌', '아침식사'],
        isVerified: true
    },
    // 샐러드 카테고리
    {
        name: '샐러디 강남점',
        category: '샐러드',
        mainCategory: '샐러드',
        subCategory: null,
        address: '서울특별시 강남구 테헤란로 151',
        location: {
            type: 'Point',
            coordinates: [127.0295, 37.5046]
        },
        phone: '02-6203-5555',
        rating: 4.3,
        reviewCount: 2345,
        priceRange: '보통',
        openingHours: '08:00 - 21:00',
        description: '신선한 샐러드 전문점입니다.',
        menuItems: [
            { name: '시저샐러드', price: 9900, description: '클래식 시저샐러드', isPopular: true },
            { name: '치킨샐러드', price: 11900, description: '그릴드 치킨이 들어간 샐러드' },
            { name: '연어샐러드', price: 14900, description: '훈제연어가 들어간 샐러드', isPopular: true }
        ],
        tags: ['샐러드', '헬시', '강남', '다이어트'],
        isVerified: true
    },
    // 멕시칸 카테고리
    {
        name: 'VIPS 타코벨',
        category: '멕시칸',
        mainCategory: '멕시칸',
        subCategory: null,
        address: '서울특별시 강남구 강남대로 390',
        location: {
            type: 'Point',
            coordinates: [127.0287, 37.4967]
        },
        phone: '02-508-7777',
        rating: 4.0,
        reviewCount: 1987,
        priceRange: '보통',
        openingHours: '11:00 - 22:00',
        description: '멕시칸 패스트푸드 전문점입니다.',
        menuItems: [
            { name: '크런치랩', price: 6900, description: '바삭한 멕시칸 랩', isPopular: true },
            { name: '타코', price: 3500, description: '전통 멕시칸 타코' },
            { name: '나초', price: 4900, description: '치즈 디핑소스와 함께', isPopular: true }
        ],
        tags: ['멕시칸', '타코', '강남', '패스트푸드'],
        isVerified: true
    }
];

// 데이터베이스 시드 함수
const seedDatabase = async () => {
    try {
        console.log('🌱 데이터베이스 시드 시작...');
        
        // 기존 데이터 삭제
        await User.deleteMany({});
        await Restaurant.deleteMany({});
        console.log('🗑️ 기존 데이터 삭제 완료');
        
        // 테스트 사용자 생성
        const users = await User.insertMany(testUsers);
        console.log(`👥 ${users.length}개의 테스트 사용자 생성 완료`);
        
        // 테스트 맛집 생성
        const restaurants = await Restaurant.insertMany(testRestaurants);
        console.log(`🍽️ ${restaurants.length}개의 테스트 맛집 생성 완료`);
        
        // 첫 번째 사용자에게 일부 맛집을 북마크/즐겨찾기로 추가
        const firstUser = users[0];
        const firstRestaurant = restaurants[0];
        const secondRestaurant = restaurants[1];
        const thirdRestaurant = restaurants[3]; // 스시조
        
        firstUser.bookmarks.push(firstRestaurant._id, secondRestaurant._id);
        firstUser.favorites.push(thirdRestaurant._id);
        await firstUser.save();
        
        console.log('🔖 테스트 사용자에게 북마크/즐겨찾기 추가 완료');
        
        // 통계 출력
        console.log('\n📊 데이터베이스 시드 완료 통계:');
        console.log(`- 사용자: ${users.length}명`);
        console.log(`- 맛집: ${restaurants.length}개`);
        console.log(`- 카테고리: ${[...new Set(restaurants.map(r => r.category))].join(', ')}`);
        console.log(`- 평균 평점: ${(restaurants.reduce((sum, r) => sum + r.rating, 0) / restaurants.length).toFixed(1)}`);
        
        console.log('\n🎯 테스트 계정 정보:');
        console.log('이메일: test@example.com');
        console.log('비밀번호: password123');
        console.log('이메일: foodie@example.com');
        console.log('비밀번호: foodie123');
        
        console.log('\n✅ 데이터베이스 시드 성공적으로 완료!');
        
    } catch (error) {
        console.error('❌ 데이터베이스 시드 실패:', error);
        throw error;
    }
};

// 스크립트 실행
const runSeed = async () => {
    try {
        await connectDB();
        await seedDatabase();
        process.exit(0);
    } catch (error) {
        console.error('시드 실행 오류:', error);
        process.exit(1);
    }
};

// 스크립트가 직접 실행될 때만 시드 실행
if (require.main === module) {
    runSeed();
}

module.exports = { seedDatabase };