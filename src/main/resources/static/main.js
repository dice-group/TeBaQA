function SubForm(s) {
    $.ajax({
        beforeSend: function () {
            $('#loaderDiv').show()
        },
        url: 'qa-simple',
        timeout: 30000,
        type: 'post',
        data: {
            'query': s,
            'lang': 'en'
        },
        success: function (msg) {
            $('#loaderDiv').hide();
            var answer = JSON.parse(msg)['answers'];
            var ul = $("#answers").find("ul");
            ul.empty();
            if (!$.isArray(answer) || !answer.length) {
                ul.append('<li><span class="tab">No answer were found.</span></li>');
            } else {
                for (var i in answer) {
                    if ({}.hasOwnProperty.call(answer, i)) {
                        ul.append('<li><span class="tab">' + answer[i] + '</span></li>');
                    }
                }
            }
        },
        error: function (xhr, status, error) {
            $('#loaderDiv').hide();
            console.error('Error while executing AJAX call: ' + xhr.responseText);
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
        SubForm($('#search-bar').val());
        return false;
    });
}
