function submitForm(s) {
    $.ajax({
        beforeSend() {
            $('#loaderDiv').show();
        },
        url: 'qa-simple',
        timeout: 30000,
        type: 'post',
        data: {
            'query': s,
            'lang': 'en'
        },
        success(msg) {
            $('#loaderDiv').hide();
            const answer = JSON.parse(msg)['answers'];
            const ul = $("#answers").find("ul");
            ul.empty();
            if (!$.isArray(answer) || !answer.length) {
                ul.append('<li><span class="tab">No answer were found.</span></li>');
            } else {
                for (let i in answer) {
                    if (answer.hasOwnProperty(i)) {
                        ul.append('<li><span class="tab">' + answer[i] + '</span></li>');
                    }
                }
            }
        },
        error() {
            $('#loaderDiv').hide();
            alert('Error while sending request. Please try again later!');
        }
    }).fail(function (jqXHR, textStatus) {
        if (textStatus === 'timeout') {
            alert('Timeout reached. Please try again later!');
        }
    });
}

function init() {
    $('#loaderDiv').hide();
    $('#search-form-id').submit(function () {
        submitForm($('#search-bar').val());
        return false;
    });
}
