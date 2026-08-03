package com.clanmanager.clanmanager.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class KoreanFortuneService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final List<FourCharacterFortune> FORTUNES = List.of(
            new FourCharacterFortune("전화위복", "轉禍爲福", "어려운 일이 오히려 좋은 결과로 바뀝니다.", "뜻밖의 변화가 기회가 될 수 있으니 침착하게 흐름을 살펴보세요."),
            new FourCharacterFortune("일취월장", "日就月將", "날마다 달마다 꾸준히 발전합니다.", "작은 노력도 쌓이면 눈에 띄는 성과가 됩니다. 미뤄둔 일을 시작하기 좋습니다."),
            new FourCharacterFortune("유비무환", "有備無患", "미리 준비하면 걱정할 일이 없습니다.", "서두르기보다 한 번 더 확인하면 실수를 피하고 좋은 결과를 얻습니다."),
            new FourCharacterFortune("고진감래", "苦盡甘來", "고생 끝에 즐거움이 찾아옵니다.", "지금까지 참아온 일이 결실을 맺을 수 있으니 마지막까지 힘을 내세요."),
            new FourCharacterFortune("새옹지마", "塞翁之馬", "좋고 나쁜 일은 쉽게 단정할 수 없습니다.", "눈앞의 결과에 흔들리지 말고 여유 있게 다음 기회를 기다려보세요."),
            new FourCharacterFortune("금상첨화", "錦上添花", "좋은 일에 좋은 일이 더해집니다.", "기분 좋은 소식이나 도움을 받을 가능성이 높습니다. 주변과 기쁨을 나누세요."),
            new FourCharacterFortune("수적천석", "水滴穿石", "물방울이 쌓여 돌을 뚫습니다.", "꾸준함이 가장 큰 행운입니다. 오늘 정한 작은 목표를 끝까지 지켜보세요."),
            new FourCharacterFortune("인화단결", "人和團結", "사람들이 화합하여 하나로 뭉칩니다.", "혼자 해결하기보다 주변 사람과 힘을 합치면 일이 쉽게 풀립니다."),
            new FourCharacterFortune("대기만성", "大器晩成", "큰 인물은 오랜 시간과 노력 끝에 완성됩니다.", "결과가 늦어도 조급해하지 마세요. 지금의 경험이 큰 성과를 준비하고 있습니다."),
            new FourCharacterFortune("파죽지세", "破竹之勢", "대나무를 쪼개듯 거침없이 나아갑니다.", "결정한 일이 있다면 자신 있게 추진하세요. 오늘은 행동력이 좋은 흐름을 만듭니다."),
            new FourCharacterFortune("청운지지", "靑雲之志", "높은 뜻과 큰 포부를 품습니다.", "평소보다 목표를 크게 잡아도 좋습니다. 새로운 계획을 세우기에 좋은 날입니다."),
            new FourCharacterFortune("화이부동", "和而不同", "서로 다름을 인정하면서 조화를 이룹니다.", "의견 차이가 있어도 상대의 말을 들으면 더 좋은 답을 찾을 수 있습니다."),
            new FourCharacterFortune("초지일관", "初志一貫", "처음 세운 뜻을 끝까지 지킵니다.", "처음의 목표를 다시 떠올려보세요. 흔들리지 않는 선택이 좋은 결과를 부릅니다."),
            new FourCharacterFortune("만사형통", "萬事亨通", "모든 일이 뜻대로 순조롭게 풀립니다.", "미뤄둔 연락이나 중요한 일을 처리하기 좋은 날입니다. 먼저 움직여보세요."),
            new FourCharacterFortune("동심협력", "同心協力", "마음을 하나로 모아 힘을 합칩니다.", "좋은 사람과 함께하면 기대 이상의 결과를 얻습니다. 도움을 주고받아보세요.")
    );

    public String getFortune() {
        return getFortune(LocalDate.now(KOREA));
    }

    String getFortune(LocalDate date) {
        int index = Math.floorMod(date.toEpochDay(), FORTUNES.size());
        FourCharacterFortune fortune = FORTUNES.get(index);
        return "**%s (%s)**\n뜻: %s\n오늘의 흐름: %s"
                .formatted(fortune.word(), fortune.hanja(), fortune.meaning(), fortune.reading());
    }

    private record FourCharacterFortune(String word, String hanja, String meaning, String reading) {
    }
}
