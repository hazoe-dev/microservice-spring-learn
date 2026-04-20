import http from 'k6/http';

export let options = {
    vus: 100,
    duration: '30s',
};

export default function () {
    http.get('http://localhost:8090/api/quizzes/1/questions');
}