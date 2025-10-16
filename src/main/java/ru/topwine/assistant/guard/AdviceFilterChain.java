package ru.topwine.assistant.guard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.topwine.assistant.model.enums.FilterDecision;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdviceFilterChain {

    private final List<AdviceFilter> adviceFilters;

    public Optional<String> run(AdviceContext context) {
        for (AdviceFilter filter : adviceFilters) {
            AdviceFilter.FilterResult result = filter.apply(context);
            if (result.decision() == FilterDecision.RESPOND_IMMEDIATELY) {
                return result.immediateReply();
            }
        }
        return Optional.empty();
    }
}