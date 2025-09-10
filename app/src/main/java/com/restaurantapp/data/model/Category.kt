package com.restaurantapp.data.model

data class Category(
    val name: String,
    val subCategories: List<String> = emptyList(),
    val hasSubCategories: Boolean = subCategories.isNotEmpty()
)

object CategoryData {
    
    // 전체 카테고리 목록 (23개)
    val ALL_CATEGORIES = listOf(
        "한식", "일식", "버거", "도시락", "치킨", "분식", "돈까스", 
        "족발/보쌈", "찜/탕", "구이", "피자", "중식", "회/해물", 
        "양식", "커피/차", "디저트", "간식", "아시안", "샌드위치", 
        "샐러드", "멕시칸", "죽"
    )
    
    // 카테고리와 하위 카테고리 매핑
    val CATEGORY_MAP = mapOf(
        "한식" to Category(
            name = "한식",
            subCategories = listOf("냉면", "국밥", "반찬", "찜닭", "칼국수", "감자탕", "부대찌개", "김치찜", "삼겹살", "김치찌개")
        ),
        "치킨" to Category(
            name = "치킨",
            subCategories = listOf("닭강정", "교촌", "BHC", "지코바", "BBQ", "푸라닭", "굽네치킨", "노랑통닭", "바른치킨", "60계치킨")
        ),
        "분식" to Category(
            name = "분식",
            subCategories = listOf("떡볶이", "김밥", "우동", "순대", "동대문엽기떡볶이", "신전떡볶이", "배떡", "김가네")
        ),
        "돈까스" to Category(
            name = "돈까스",
            subCategories = emptyList()
        ),
        "족발/보쌈" to Category(
            name = "족발/보쌈",
            subCategories = emptyList()
        ),
        "찜/탕" to Category(
            name = "찜/탕",
            subCategories = listOf("찜닭", "감자탕", "김치찜", "아구찜", "갈비탕", "설렁탕", "삼계탕", "갈비찜", "닭볶음탕", "해물찜")
        ),
        "구이" to Category(
            name = "구이",
            subCategories = listOf("곱창", "삼겹살", "갈비")
        ),
        "피자" to Category(
            name = "피자",
            subCategories = listOf("화덕피자", "피자헛", "피자스쿨", "잭슨피자", "파파존스", "피자알볼로", "피자마루", "청년피자", "고피자")
        ),
        "중식" to Category(
            name = "중식",
            subCategories = listOf("마라탕", "짜장면", "양꼬치", "홍콩반점")
        ),
        "일식" to Category(
            name = "일식",
            subCategories = emptyList()
        ),
        "회/해물" to Category(
            name = "회/해물",
            subCategories = emptyList()
        ),
        "양식" to Category(
            name = "양식",
            subCategories = emptyList()
        ),
        "커피/차" to Category(
            name = "커피/차",
            subCategories = emptyList()
        ),
        "디저트" to Category(
            name = "디저트",
            subCategories = listOf("와플", "케이크", "토스트", "빙수", "아이스크림", "도넛", "공차", "설빙", "배스킨라빈스", "에그드랍")
        ),
        "간식" to Category(
            name = "간식",
            subCategories = emptyList()
        ),
        "아시안" to Category(
            name = "아시안",
            subCategories = emptyList()
        ),
        "샌드위치" to Category(
            name = "샌드위치",
            subCategories = emptyList()
        ),
        "샐러드" to Category(
            name = "샐러드",
            subCategories = emptyList()
        ),
        "버거" to Category(
            name = "버거",
            subCategories = listOf("수제버거", "맥도날드", "버거킹", "롯데리아", "맘스터치", "KFC", "프랭크버거", "쉐이크쉑", "노브렌드")
        ),
        "멕시칸" to Category(
            name = "멕시칸",
            subCategories = emptyList()
        ),
        "도시락" to Category(
            name = "도시락",
            subCategories = emptyList()
        ),
        "죽" to Category(
            name = "죽",
            subCategories = emptyList()
        )
    )
    
    // 메인 페이지에 표시할 주요 카테고리 (첫 번째 줄)
    val MAIN_CATEGORIES = listOf("전체", "한식", "중식", "일식", "양식", "치킨")
    
    // 확장 카테고리 (더보기를 누르면 표시)
    val EXPANDED_CATEGORIES = ALL_CATEGORIES.filter { !MAIN_CATEGORIES.contains(it) || it == "치킨" }
    
    fun getCategory(name: String): Category? {
        return CATEGORY_MAP[name]
    }
    
    fun hasSubCategories(categoryName: String): Boolean {
        return CATEGORY_MAP[categoryName]?.hasSubCategories ?: false
    }
    
    fun getSubCategories(categoryName: String): List<String> {
        return CATEGORY_MAP[categoryName]?.subCategories ?: emptyList()
    }
}