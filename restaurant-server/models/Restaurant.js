const mongoose = require('mongoose');

const restaurantSchema = new mongoose.Schema({
    name: {
        type: String,
        required: [true, '맛집 이름은 필수입니다'],
        trim: true,
        maxlength: [100, '맛집 이름은 최대 100자까지 입니다']
    },
    // 기존 호환성을 위한 category (mainCategory와 동일)
    category: {
        type: String,
        required: [true, '카테고리는 필수입니다'],
        enum: {
            values: ['한식', '일식', '버거', '도시락', '치킨', '분식', '돈까스', '족발/보쌈', '찜/탕', '구이', '피자', '중식', '회/해물', '양식', '커피/차', '디저트', '간식', '아시안', '샌드위치', '샐러드', '멕시칸', '죽'],
            message: '유효하지 않은 카테고리입니다'
        }
    },
    // 새로운 카테고리 시스템
    mainCategory: {
        type: String,
        required: [true, '상위 카테고리는 필수입니다'],
        enum: {
            values: ['한식', '일식', '버거', '도시락', '치킨', '분식', '돈까스', '족발/보쌈', '찜/탕', '구이', '피자', '중식', '회/해물', '양식', '커피/차', '디저트', '간식', '아시안', '샌드위치', '샐러드', '멕시칸', '죽'],
            message: '유효하지 않은 상위 카테고리입니다'
        }
    },
    subCategory: {
        type: String,
        trim: true
    },
    address: {
        type: String,
        required: [true, '주소는 필수입니다'],
        trim: true
    },
    location: {
        type: {
            type: String,
            enum: ['Point'],
            required: true,
            default: 'Point'
        },
        coordinates: {
            type: [Number], // [longitude, latitude]
            required: [true, '좌표는 필수입니다'],
            validate: {
                validator: function(coords) {
                    return coords.length === 2 && 
                           coords[0] >= -180 && coords[0] <= 180 && // longitude
                           coords[1] >= -90 && coords[1] <= 90;    // latitude
                },
                message: '올바른 좌표 형식이 아닙니다 (경도: -180~180, 위도: -90~90)'
            }
        }
    },
    phone: {
        type: String,
        trim: true,
        validate: {
            validator: function(phone) {
                if (!phone) return true; // 선택 필드이므로 빈 값 허용
                return /^[0-9-+\s()]{8,20}$/.test(phone);
            },
            message: '올바른 전화번호 형식이 아닙니다'
        }
    },
    rating: {
        type: Number,
        default: 0.0,
        min: [0, '평점은 0 이상이어야 합니다'],
        max: [5, '평점은 5 이하여야 합니다'],
        set: function(val) {
            return Math.round(val * 10) / 10; // 소수점 첫째 자리까지
        }
    },
    reviewCount: {
        type: Number,
        default: 0,
        min: [0, '리뷰 개수는 0 이상이어야 합니다']
    },
    priceRange: {
        type: String,
        enum: {
            values: ['저렴', '보통', '비싸', '고급'],
            message: '유효하지 않은 가격대입니다'
        },
        default: '보통'
    },
    openingHours: {
        type: String,
        trim: true
    },
    imageUrl: {
        type: String,
        trim: true,
        validate: {
            validator: function(url) {
                if (!url) return true; // 선택 필드이므로 빈 값 허용
                return /^https?:\/\/.+\.(jpg|jpeg|png|gif|webp)$/i.test(url);
            },
            message: '올바른 이미지 URL 형식이 아닙니다'
        }
    },
    description: {
        type: String,
        trim: true,
        maxlength: [1000, '설명은 최대 1000자까지 입니다']
    },
    features: [{
        type: String,
        enum: ['주차가능', '24시간', '배달가능', '포장가능', '카드결제', '무선인터넷', '단체석', '금연']
    }],
    menuItems: [{
        name: {
            type: String,
            required: true,
            trim: true
        },
        price: {
            type: Number,
            required: true,
            min: 0
        },
        description: {
            type: String,
            trim: true
        },
        imageUrl: String,
        isPopular: {
            type: Boolean,
            default: false
        }
    }],
    businessHours: {
        monday: { open: String, close: String },
        tuesday: { open: String, close: String },
        wednesday: { open: String, close: String },
        thursday: { open: String, close: String },
        friday: { open: String, close: String },
        saturday: { open: String, close: String },
        sunday: { open: String, close: String }
    },
    tags: [{
        type: String,
        trim: true,
        lowercase: true
    }],
    source: {
        type: String,
        enum: ['manual', 'kakao', 'google', 'naver'],
        default: 'manual'
    },
    externalId: {
        type: String, // 외부 API의 고유 ID
        sparse: true
    },
    isVerified: {
        type: Boolean,
        default: false
    },
    isActive: {
        type: Boolean,
        default: true
    },
    owner: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User'
    },
    lastUpdated: {
        type: Date,
        default: Date.now
    }
}, {
    timestamps: true,
    toJSON: { virtuals: true },
    toObject: { virtuals: true }
});

// 인덱스 설정
restaurantSchema.index({ location: '2dsphere' }); // 지리적 검색용
restaurantSchema.index({ name: 'text', description: 'text', tags: 'text' }); // 텍스트 검색용
restaurantSchema.index({ category: 1, rating: -1 });
restaurantSchema.index({ rating: -1, reviewCount: -1 });
restaurantSchema.index({ createdAt: -1 });
restaurantSchema.index({ priceRange: 1 });
restaurantSchema.index({ isActive: 1, isVerified: 1 });
restaurantSchema.index({ source: 1, externalId: 1 }, { sparse: true });

// 가상 필드: 평균 메뉴 가격
restaurantSchema.virtual('averageMenuPrice').get(function() {
    if (!this.menuItems || this.menuItems.length === 0) return 0;
    const total = this.menuItems.reduce((sum, item) => sum + item.price, 0);
    return Math.round(total / this.menuItems.length);
});

// 가상 필드: 인기 메뉴
restaurantSchema.virtual('popularMenus').get(function() {
    if (!this.menuItems) return [];
    return this.menuItems.filter(item => item.isPopular);
});

// 가상 필드: 현재 운영 상태
restaurantSchema.virtual('isOpen').get(function() {
    if (!this.businessHours) return null;
    
    const now = new Date();
    const days = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'];
    const today = days[now.getDay()];
    const currentTime = now.getHours().toString().padStart(2, '0') + ':' + 
                       now.getMinutes().toString().padStart(2, '0');
    
    const todayHours = this.businessHours[today];
    if (!todayHours || !todayHours.open || !todayHours.close) {
        return false; // 운영시간 정보 없음
    }
    
    return currentTime >= todayHours.open && currentTime <= todayHours.close;
});

// 미들웨어: 저장 전 lastUpdated 업데이트
restaurantSchema.pre('save', function(next) {
    this.lastUpdated = Date.now();
    next();
});

// 미들웨어: 삭제 전 관련 데이터 정리
restaurantSchema.pre('remove', async function(next) {
    try {
        // 사용자의 북마크/즐겨찾기에서 이 맛집 제거
        await mongoose.model('User').updateMany(
            { $or: [{ bookmarks: this._id }, { favorites: this._id }] },
            { $pull: { bookmarks: this._id, favorites: this._id } }
        );
        next();
    } catch (error) {
        next(error);
    }
});

// 메서드: 거리 계산 (단위: km)
restaurantSchema.methods.getDistanceFrom = function(lat, lng) {
    const R = 6371; // 지구 반지름 (km)
    const dLat = this.toRadians(lat - this.location.coordinates[1]);
    const dLng = this.toRadians(lng - this.location.coordinates[0]);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(this.toRadians(this.location.coordinates[1])) * 
              Math.cos(this.toRadians(lat)) *
              Math.sin(dLng / 2) * Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round((R * c) * 100) / 100; // 소수점 둘째 자리까지
};

restaurantSchema.methods.toRadians = function(degrees) {
    return degrees * (Math.PI / 180);
};

// 메서드: 평점 업데이트
restaurantSchema.methods.updateRating = function(newRating) {
    const totalRating = (this.rating * this.reviewCount) + newRating;
    this.reviewCount += 1;
    this.rating = totalRating / this.reviewCount;
    return this.save();
};

// 메서드: 메뉴 추가
restaurantSchema.methods.addMenuItem = function(menuItem) {
    this.menuItems.push(menuItem);
    return this.save();
};

// 메서드: 메뉴 제거
restaurantSchema.methods.removeMenuItem = function(menuItemId) {
    this.menuItems.id(menuItemId).remove();
    return this.save();
};

// 정적 메서드: 주변 맛집 검색
restaurantSchema.statics.findNearby = function(lat, lng, maxDistance = 5000) {
    return this.find({
        location: {
            $near: {
                $geometry: {
                    type: 'Point',
                    coordinates: [lng, lat]
                },
                $maxDistance: maxDistance // 미터 단위
            }
        },
        isActive: true
    });
};

// 정적 메서드: 텍스트 검색
restaurantSchema.statics.searchByText = function(query) {
    return this.find({
        $text: { $search: query },
        isActive: true
    }).sort({ score: { $meta: 'textScore' } });
};

// 정적 메서드: 카테고리별 인기 맛집
restaurantSchema.statics.findPopularByCategory = function(category, limit = 10) {
    const query = { isActive: true };
    if (category && category !== '전체') {
        query.category = category;
    }
    
    return this.find(query)
               .sort({ rating: -1, reviewCount: -1 })
               .limit(limit);
};

// 정적 메서드: 활성 맛집만 조회
restaurantSchema.statics.findActive = function() {
    return this.find({ isActive: true });
};

module.exports = mongoose.model('Restaurant', restaurantSchema);