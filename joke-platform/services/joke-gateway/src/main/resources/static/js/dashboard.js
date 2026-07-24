"use strict";

const elements = {
    generateButton:
        document.querySelector("#generate-button"),

    refreshStatisticsButton:
        document.querySelector(
            "#refresh-statistics-button"
        ),

    resetStatisticsButton:
        document.querySelector(
            "#reset-statistics-button"
        ),

    category:
        document.querySelector("#category"),

    chaosMode:
        document.querySelector("#chaos-mode"),

    seed:
        document.querySelector("#seed"),

    requestStatus:
        document.querySelector("#request-status"),

    errorPanel:
        document.querySelector("#error-panel"),

    errorTitle:
        document.querySelector("#error-title"),

    errorMessage:
        document.querySelector("#error-message"),

    errorDetails:
        document.querySelector("#error-details"),

    emptyState:
        document.querySelector("#empty-state"),

    jokeResult:
        document.querySelector("#joke-result"),

    categoryBadge:
        document.querySelector("#category-badge"),

    setupText:
        document.querySelector("#setup-text"),

    punchlineText:
        document.querySelector("#punchline-text"),

    reactionName:
        document.querySelector("#reaction-name"),

    reactionDescription:
        document.querySelector(
            "#reaction-description"
        ),

    reactionScore:
        document.querySelector("#reaction-score"),

    chaosCard:
        document.querySelector("#chaos-card"),

    chaosRequested:
        document.querySelector("#chaos-requested"),

    chaosApplied:
        document.querySelector("#chaos-applied"),

    chaosDelay:
        document.querySelector("#chaos-delay"),

    chaosInstance:
        document.querySelector("#chaos-instance"),

    chaosOddity:
        document.querySelector("#chaos-oddity"),

    traceGateway:
        document.querySelector("#trace-gateway"),

    traceGenerator:
        document.querySelector("#trace-generator"),

    tracePunchline:
        document.querySelector("#trace-punchline"),

    traceAudience:
        document.querySelector("#trace-audience"),

    traceChaos:
        document.querySelector("#trace-chaos"),

    totalReactions:
        document.querySelector("#total-reactions"),

    totalScore:
        document.querySelector("#total-score"),

    averageScore:
        document.querySelector("#average-score"),

    statisticsProvider:
        document.querySelector(
            "#statistics-provider"
        ),

    reactionStatistics:
        document.querySelector(
            "#reaction-statistics"
        )
};

elements.generateButton.addEventListener(
    "click",
    generateJoke
);

elements.refreshStatisticsButton.addEventListener(
    "click",
    refreshStatistics
);

elements.resetStatisticsButton.addEventListener(
    "click",
    resetStatistics
);

async function generateJoke() {
    clearError();
    setLoading(true);

    const seedValue =
        elements.seed.value.trim();

    const request = {
        category:
            elements.category.value || null,

        chaosMode:
            elements.chaosMode.value,

        seed:
            seedValue === ""
                ? null
                : Number(seedValue)
    };

    try {
        const response = await fetch(
            "api/v1/jokes",
            {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/json"
                },
                body: JSON.stringify(request)
            }
        );

        const payload = await readJson(response);

        if (!response.ok) {
            throw createHttpError(
                response,
                payload
            );
        }

        renderJoke(payload);
        setStatus("Success", "success");

        await refreshStatistics();
    } catch (error) {
        showError(error);
        setStatus("Failed", "error");
    } finally {
        setLoading(false);
    }
}

async function refreshStatistics() {
    try {
        const response = await fetch(
            "api/v1/statistics"
        );

        const payload = await readJson(response);

        if (!response.ok) {
            throw createHttpError(
                response,
                payload
            );
        }

        renderStatistics(payload);
    } catch (error) {
        showError(error);
    }
}

async function resetStatistics() {
    const confirmed = window.confirm(
        "Reset all audience statistics?"
    );

    if (!confirmed) {
        return;
    }

    clearError();

    try {
        const response = await fetch(
            "api/v1/statistics",
            {
                method: "DELETE"
            }
        );

        if (!response.ok) {
            const payload =
                await readJson(response);

            throw createHttpError(
                response,
                payload
            );
        }

        await refreshStatistics();
    } catch (error) {
        showError(error);
    }
}

function renderJoke(payload) {
    elements.emptyState.classList.add("hidden");
    elements.jokeResult.classList.remove("hidden");

    elements.categoryBadge.textContent =
        payload.category;

    elements.setupText.textContent =
        payload.setup;

    elements.punchlineText.textContent =
        payload.punchline;

    elements.reactionName.textContent =
        humanize(payload.audience.reaction);

    elements.reactionDescription.textContent =
        payload.audience.description;

    elements.reactionScore.textContent =
        formatSignedNumber(
            payload.audience.score
        );

    elements.traceGateway.textContent =
        payload.trace.gateway;

    elements.traceGenerator.textContent =
        payload.trace.jokeGenerator;

    elements.tracePunchline.textContent =
        payload.trace.punchlineService;

    elements.traceAudience.textContent =
        payload.trace.audienceService;

    elements.traceChaos.textContent =
        payload.trace.chaosComedian;

    renderChaos(payload.chaos);
}

function renderChaos(chaos) {
    if (!chaos.invoked) {
        elements.chaosCard.classList.add(
            "hidden"
        );
        return;
    }

    elements.chaosCard.classList.remove(
        "hidden"
    );

    elements.chaosRequested.textContent =
        chaos.requestedMode;

    elements.chaosApplied.textContent =
        chaos.appliedMode;

    elements.chaosDelay.textContent =
        `${chaos.delayMs} ms`;

    elements.chaosInstance.textContent =
        chaos.handledBy;

    const hasOddity =
        chaos.oddity
        && Object.keys(chaos.oddity).length > 0;

    if (hasOddity) {
        elements.chaosOddity.textContent =
            JSON.stringify(
                chaos.oddity,
                null,
                2
            );

        elements.chaosOddity.classList.remove(
            "hidden"
        );
    } else {
        elements.chaosOddity.classList.add(
            "hidden"
        );
    }
}

function renderStatistics(statistics) {
    elements.totalReactions.textContent =
        statistics.totalReactions;

    elements.totalScore.textContent =
        statistics.totalScore;

    elements.averageScore.textContent =
        Number(
            statistics.averageScore
        ).toFixed(2);

    elements.statisticsProvider.textContent =
        statistics.statisticsProvider;

    elements.reactionStatistics.replaceChildren();

    const reactions =
        statistics.reactions || {};

    const maximum =
        Math.max(
            1,
            ...Object.values(reactions)
        );

    Object.entries(reactions).forEach(
        ([reaction, count]) => {
            const row =
                document.createElement("div");

            row.className = "reaction-row";

            const label =
                document.createElement("span");

            label.className = "reaction-label";
            label.textContent = humanize(reaction);

            const track =
                document.createElement("div");

            track.className = "reaction-track";

            const bar =
                document.createElement("div");

            bar.className = "reaction-bar";
            bar.style.width =
                `${(count / maximum) * 100}%`;

            track.append(bar);

            const value =
                document.createElement("strong");

            value.textContent = count;

            row.append(label, track, value);

            elements.reactionStatistics.append(row);
        }
    );
}

function setLoading(loading) {
    elements.generateButton.disabled = loading;

    if (loading) {
        setStatus("Working", "loading");
    }
}

function setStatus(text, type) {
    elements.requestStatus.textContent = text;

    elements.requestStatus.className =
        `status-badge status-${type}`;
}

function showError(error) {
    elements.errorPanel.classList.remove(
        "hidden"
    );

    elements.errorTitle.textContent =
        error.title || "Request failed";

    elements.errorMessage.textContent =
        error.message
        || "An unexpected error occurred.";

    elements.errorDetails.textContent =
        error.details
            ? JSON.stringify(
                error.details,
                null,
                2
            )
            : "";
}

function clearError() {
    elements.errorPanel.classList.add(
        "hidden"
    );

    elements.errorMessage.textContent = "";
    elements.errorDetails.textContent = "";
}

async function readJson(response) {
    const contentType =
        response.headers.get("content-type")
        || "";

    if (!contentType.includes(
        "application/json"
    )) {
        return null;
    }

    return response.json();
}

function createHttpError(response, payload) {
    const error = new Error(
        payload?.message
        || `HTTP ${response.status}`
    );

    error.title =
        payload?.error
        || "Request failed";

    error.details =
        payload?.details
        || payload?.violations
        || null;

    return error;
}

function humanize(value) {
    return String(value || "")
        .replaceAll("-", " ")
        .replaceAll("_", " ");
}

function formatSignedNumber(value) {
    const number = Number(value);

    return number > 0
        ? `+${number}`
        : `${number}`;
}

refreshStatistics();
