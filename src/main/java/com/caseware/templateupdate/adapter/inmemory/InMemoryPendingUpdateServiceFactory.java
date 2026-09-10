package com.caseware.templateupdate.adapter.inmemory;

import com.caseware.templateupdate.app.PendingUpdateService;
import com.caseware.templateupdate.summary.DeterministicSummarizer;

/**
 * Wires the slice with in-memory adapters. Fine for tests and the demo; not a
 * persistence story. Production would bind the same service to Dynamo/SQS/etc.
 */
public final class InMemoryPendingUpdateServiceFactory {
    private InMemoryPendingUpdateServiceFactory() {}

    public static PendingUpdateService create(InMemoryTemplateDiffer differ) {
        return new PendingUpdateService(
                new InMemoryEngagementTemplateRegistry(),
                new InMemoryTemplateCatalog(),
                differ,
                new DeterministicSummarizer(),
                new InMemoryPendingUpdateStore(),
                new InMemorySummaryCache()
        );
    }

    public static Harness harness() {
        InMemoryTemplateDiffer differ = new InMemoryTemplateDiffer();
        InMemorySummaryCache cache = new InMemorySummaryCache();
        InMemoryEngagementTemplateRegistry registry = new InMemoryEngagementTemplateRegistry();
        InMemoryTemplateCatalog catalog = new InMemoryTemplateCatalog();
        InMemoryPendingUpdateStore pending = new InMemoryPendingUpdateStore();
        PendingUpdateService service = new PendingUpdateService(
                registry,
                catalog,
                differ,
                new DeterministicSummarizer(),
                pending,
                cache
        );
        return new Harness(service, differ, cache, registry, catalog, pending);
    }

    public record Harness(
            PendingUpdateService service,
            InMemoryTemplateDiffer differ,
            InMemorySummaryCache cache,
            InMemoryEngagementTemplateRegistry registry,
            InMemoryTemplateCatalog catalog,
            InMemoryPendingUpdateStore pending
    ) {}
}
