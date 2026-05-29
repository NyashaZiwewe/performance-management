function parseAjaxResponse(data) {
    if (typeof data === "string") {
        try {
            return JSON.parse(data);
        } catch (e) {
            return {};
        }
    }
    return data || {};
}

function getCsrfToken() {
    var cookieParts = document.cookie ? document.cookie.split(";") : [];
    for (var i = 0; i < cookieParts.length; i++) {
        var cookie = cookieParts[i].trim();
        if (cookie.indexOf("XSRF-TOKEN=") === 0) {
            return decodeURIComponent(cookie.substring("XSRF-TOKEN=".length));
        }
    }
    var csrfMeta = document.querySelector("meta[name='_csrf']");
    return csrfMeta ? csrfMeta.getAttribute("content") : null;
}

function getScoreStatusElement() {
    return document.getElementById("scoreSaveStatus");
}

function setScoreStatus(message, isError) {
    var statusElement = getScoreStatusElement();
    if (!statusElement) {
        if (message && isError) {
            alert(message);
        }
        return;
    }

    statusElement.textContent = message || "";
    statusElement.classList.remove("success");
    statusElement.classList.remove("error");
    if (message) {
        statusElement.classList.add(isError ? "error" : "success");
    }
}

function notifyScoreRangeError() {
    setScoreStatus("Score must be between 1 and 5.", true);
}

function isScoreOutOfRange(value) {
    if (value === "" || Number(value) === 0.0) {
        return false;
    }
    return Number(value) < 1 || Number(value) > 5;
}

function sendScoreRequest(url, payload) {
    var csrfToken = getCsrfToken();
    if (csrfToken) {
        payload._csrf = csrfToken;
    }

    $.ajax({
        url: url,
        method: "POST",
        data: payload,
        beforeSend: function (xhr) {
            if (csrfToken) {
                xhr.setRequestHeader("X-XSRF-TOKEN", csrfToken);
            }
        },
        success: function (data) {
            var response = parseAjaxResponse(data);
            if (response.alreadyExists === true) {
                setScoreStatus("Score already exists for this target and reporting date.", true);
                return;
            }
            if (response.captureBlocked === true) {
                setScoreStatus(response.message || "Score capture is blocked for the current stage.", true);
                return;
            }
            setScoreStatus("Changes saved.", false);
        },
        error: function (xhr) {
            var response = parseAjaxResponse(xhr.responseText);
            if (response.message) {
                setScoreStatus(response.message, true);
                return;
            }
            setScoreStatus("Save failed. Please retry.", true);
        }
    });
}

function saveStandardScore(targetId) {
    var actualField = document.getElementById("actual" + targetId);
    var evidenceField = document.getElementById("evidence" + targetId);
    var justificationField = document.getElementById("justification" + targetId);
    if (!actualField || !evidenceField || !justificationField) {
        setScoreStatus("Unable to locate score fields.", true);
        return;
    }

    sendScoreRequest("/scorecards/save-standard-score", {
        targetId: targetId,
        actual: actualField.value,
        evidence: evidenceField.value,
        justification: justificationField.value
    });
}

function saveValueBasedScore(targetId, scoreFieldIdPrefix, scoreFieldName, endpoint, justificationFieldIdPrefix) {
    var scoreField = document.getElementById(scoreFieldIdPrefix + targetId);
    if (!scoreField) {
        setScoreStatus("Score field could not be found.", true);
        return false;
    }
    var scoreValue = scoreField.value;
    if (isScoreOutOfRange(scoreValue)) {
        notifyScoreRangeError();
        return false;
    }

    var payload = {targetId: targetId};
    payload[scoreFieldName] = scoreValue;

    if (justificationFieldIdPrefix) {
        var justificationField = document.getElementById(justificationFieldIdPrefix + targetId);
        payload.justification = justificationField ? justificationField.value : "";
    }

    sendScoreRequest(endpoint, payload);
    return true;
}

var commentDebounceTimers = {};

function saveComment(userType) {
    var scorecardIdField = document.getElementById("id");
    if (!scorecardIdField) {
        return;
    }

    var commentFieldMap = {
        owner: "ownerComment",
        supervisor: "supervisorComment",
        moderator: "moderatorComment"
    };
    var normalizedUserType = (userType || "").toLowerCase();
    var commentFieldId = commentFieldMap[normalizedUserType];
    if (!commentFieldId) {
        return;
    }

    var commentField = document.getElementById(commentFieldId);
    if (!commentField) {
        return;
    }

    if (commentDebounceTimers[normalizedUserType]) {
        clearTimeout(commentDebounceTimers[normalizedUserType]);
    }
    commentDebounceTimers[normalizedUserType] = setTimeout(function () {
        var csrfToken = getCsrfToken();
        var payload = {
            scorecardId: scorecardIdField.value,
            userType: userType,
            comment: commentField.value
        };
        if (csrfToken) {
            payload._csrf = csrfToken;
        }

        $.ajax({
            url: "/scorecards/save-overall-comment",
            method: "POST",
            data: payload,
            beforeSend: function (xhr) {
                if (csrfToken) {
                    xhr.setRequestHeader("X-XSRF-TOKEN", csrfToken);
                }
            },
            success: function () {
                setScoreStatus("Comment saved.", false);
            },
            error: function () {
                setScoreStatus("Comment save failed. Please retry.", true);
            }
        });
    }, 600);
}
