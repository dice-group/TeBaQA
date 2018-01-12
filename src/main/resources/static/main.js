function SubForm(s) {
    $.ajax({
        url: '/qa-simple',
        type: 'post',
        data: {
            'query': s,
            'lang': 'en'
        },
        success: function (msg) {
            var answer = JSON.parse(msg)['answers'];
            var ul = $("#answers").find("ul");
            ul.empty();
            if (!$.isArray(answer) || !answer.length) {
                ul.append('<li><span class="tab">No answer were found.</span></li>');
            } else {
                for (var i in answer) {
                    ul.append('<li><span class="tab">' + answer[i] + '</span></li>');
                }
            }
        }
    });
}
