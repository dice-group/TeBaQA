function showSpinner() {
    $('#loaderDiv').show();
    $('#overlay').show();
}

function hideSpinner() {
    $('#loaderDiv').hide();
    $('#overlay').hide();
}

function emptyAnswerList() {
    $("#answers").find("ul").empty();
}

function submitForm(s) {
    $.ajax({
        beforeSend() {
            showSpinner();
        },
        url: 'qa-simple',
        timeout: 30000,
        type: 'post',
        data: {
            'query': s,
            'lang': 'en'
        },
        success(msg) {
            emptyAnswerList();
            const answer = JSON.parse(msg)['answers'];
            const ul = $("#answers").find("ul");
            if (!$.isArray(answer) || !answer.length) {
                ul.append('<li><span class="tab">No answer were found.</span></li>');
            } else {
                for (let i in answer) {
                    if (answer.hasOwnProperty(i)) {
                        ul.append('<li><span class="tab">' + answer[i] + '</span></li>');
                    }
                }
            }
            hideSpinner();
        },
        error() {
            emptyAnswerList();
            hideSpinner();
            alert('Error while sending request. Please try again later!');
        }
    }).fail(function (jqXHR, textStatus) {
        emptyAnswerList();
        hideSpinner();
        if (textStatus === 'timeout') {
            alert('Timeout reached. Please try again later!');
        }
    });
}

function init() {
    hideSpinner();
    $('#search-form-id').submit(function () {
        submitForm($('#search-bar').val());
        return false;
    });
}
