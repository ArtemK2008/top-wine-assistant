package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.topwine.assistant.configuration.AiProps;
import ru.topwine.assistant.guard.AdviceContext;
import ru.topwine.assistant.guard.AdviceFilterChain;
import ru.topwine.assistant.http.client.ChatClient;
import ru.topwine.assistant.http.request.OpenAiChatCompletionsRequest;
import ru.topwine.assistant.http.request.OpenAiRequestFactory;
import ru.topwine.assistant.model.ChatMessage;
import ru.topwine.assistant.service.SommelierService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SommelierServiceImpl implements SommelierService {
    private static final String SYSTEM_PROMPT = """
            Ты — дружелюбный сомелье в винном бистро. Отвечай ТОЛЬКО НА РУССКОМ.
            Требования (обязательно):
            - Не выдумывай бренды/названия хозяйств/фантазийные регионы. Используй только стили/сорта/регионы/уровни выдержки:\s
              примеры допустимых формулировок: «Риоха Крианца (Темпранильо)», «Кьянти Классико (Санджовезе)», «Пино Нуар (Бургундия)».
            - Если просят бренды — дай апелласьоны/страны/регион и ценовой ориентир (без конкретных марок).
            - Структура ответа строго:
              1) 1–3 строки в формате «Стиль/регион — коротко почему подходит (одно предложение)».
              2) «Пара: …» с 1–2 блюдами.
              3) «Уточнение: …» — ровно один короткий вопрос, только если реально не хватает данных.
            - Обязательно корректная терминология: «умеренные танины», «среднее тело», «кислотность», «дубовые ноты».
            - Без рассуждений, планов, “обоснования”, “reasoning”, “шагов”. Выводи только финальный ответ в указанном формате.
            - К красным и белым по возможности добавляй температуру подачи (красные 16–18°C, лёгкие красные 14–16°C, белые 8–12°C, игристые 6–8°C).
            - При запросах про аллергии/сульфиты: не делай медицинских утверждений; рекомендуй читать этикетку “no added sulfites” и консультироваться со специалистом.
            Примеры:
            Пользователь: «Хочу лёгкое красное к рыбе»
            Ответ:
            - Пино Нуар (Эльзас/Бургундия) — лёгкое тело и яркая кислинка, не перебьёт рыбу.
            Пара: лосось на гриле, тунец татакки.
            Уточнение: Нужен бюджет до … ₽?
            Пользователь: «Люблю сухое красное, среднее по телу»
            Ответ:
            - Риоха Крианца (Темпранильо) — среднее тело, красные ягоды, немного дуба.
            - Кьянти Классико (Санджовезе) — вишня и кислинка, хорошо с томатной кислотой.
            - Мерло (правый берег Бордо) — мягкие танины, спелая чёрная ягода.
            Пара: паста с томатным соусом, лазанья, телятина.
            """;

    private final ChatClient chatClient;
    private final AiProps aiProps;
    private final AdviceFilterChain filterChain;

    @Override
    public Mono<String> advise(String userMessage) {
        AdviceContext context = new AdviceContext(userMessage);

        Optional<String> earlyReply = filterChain.run(context);
        if (earlyReply.isPresent()) {
            return Mono.just(earlyReply.get());
        }

        int logicalCpus = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(1, logicalCpus - 1);
        threads = Math.min(threads, 16);

        OpenAiChatCompletionsRequest request = OpenAiRequestFactory.build(
                aiProps.model(),
                SYSTEM_PROMPT,
                List.of(ChatMessage.user(userMessage)),
                aiProps.temperature(),
                aiProps.maxTokens()
        );
        return chatClient.chat(request);
    }
}