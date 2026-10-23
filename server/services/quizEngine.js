const fs = require('fs');
const path = require('path');

// Cache the parsed questions
const quizCache = {
    ipl: null,
    sa20: null,
    wpl: null
};

// Fallback questions in case file reading fails on any environment
const FALLBACK_QUESTIONS = {
    easy: [
        { id: 'f1', text: 'Which team won the inaugural IPL title in 2008?', options: ['Rajasthan Royals', 'Chennai Super Kings', 'Mumbai Indians', 'Kolkata Knight Riders'], correctIndex: 0 },
        { id: 'f2', text: 'Who holds the record for the highest individual score in IPL history (175*)?', options: ['AB de Villiers', 'Chris Gayle', 'Virat Kohli', 'Brendon McCullum'], correctIndex: 1 },
        { id: 'f3', text: 'Which player has won the most IPL MVP / Player of the Tournament awards?', options: ['Virat Kohli', 'Andre Russell', 'Sunil Narine', 'Shane Watson'], correctIndex: 2 },
        { id: 'f4', text: 'What is the maximum number of overseas players allowed in an IPL playing XI?', options: ['3', '4', '5', '6'], correctIndex: 1 }
    ],
    medium: [
        { id: 'f5', text: 'Who bowled the first-ever super over in IPL history?', options: ['Kamran Khan', 'Lasith Malinga', 'Jasprit Bumrah', 'Dale Steyn'], correctIndex: 0 },
        { id: 'f6', text: 'Which bowler took the first hat-trick in IPL history?', options: ['Amit Mishra', 'Lakshmipathy Balaji', 'Yuvraj Singh', 'Makhaya Ntini'], correctIndex: 1 },
        { id: 'f7', text: 'How many teams participated in IPL 2011?', options: ['8', '9', '10', '12'], correctIndex: 2 }
    ],
    hard: [
        { id: 'f8', text: 'Who was the captain of Delhi Daredevils in IPL 2008 semifinal?', options: ['Virender Sehwag', 'Gautam Gambhir', 'Dinesh Karthik', 'Glenn McGrath'], correctIndex: 0 },
        { id: 'f9', text: 'Which uncapped player scored the first-ever IPL century by an Indian?', options: ['Paul Valthaty', 'Manish Pandey', 'Shaun Marsh', 'Ajinkya Rahane'], correctIndex: 1 },
        { id: 'f10', text: 'What was the lowest team total defended successfully in IPL history?', options: ['116', '118', '119', '120'], correctIndex: 2 }
    ]
};

/**
 * Parses the quiz text file and categorizes questions into Easy, Medium, Hard.
 */
function parseQuizFile(filePath) {
    if (!fs.existsSync(filePath)) {
        console.warn(`[QuizEngine] File not found at path: ${filePath}`);
        return null;
    }

    try {
        const content = fs.readFileSync(filePath, 'utf-8');
        const lines = content.split('\n');

        const categorizedQuestions = {
            easy: [],
            medium: [],
            hard: []
        };

        let currentCategory = 'easy'; // Default
        let currentQuestion = null;

        for (let i = 0; i < lines.length; i++) {
            const line = lines[i].trim();
            if (!line) continue;

            // Detect category change
            if (line.toLowerCase().includes('### easy')) currentCategory = 'easy';
            else if (line.toLowerCase().includes('### medium')) currentCategory = 'medium';
            else if (line.toLowerCase().includes('### hard')) currentCategory = 'hard';

            // Detect new question (starts with number and dot)
            const qMatch = line.match(/^(\d+)\.\s+(.*)/);
            if (qMatch) {
                if (currentQuestion) {
                    categorizedQuestions[currentCategory].push(currentQuestion);
                }
                currentQuestion = {
                    id: qMatch[1],
                    text: qMatch[2],
                    options: [],
                    correctIndex: -1
                };
                continue;
            }

            // Detect options (starts with A), B), C), D))
            const optMatch = line.match(/^([A-D])\)\s+(.*)/);
            if (optMatch && currentQuestion) {
                let optionText = optMatch[2];
                let isCorrect = false;

                // Check if correct (wrapped in **)
                if (optionText.startsWith('**') && optionText.endsWith('**')) {
                    isCorrect = true;
                    optionText = optionText.substring(2, optionText.length - 2).trim();
                }

                currentQuestion.options.push(optionText);
                if (isCorrect) {
                    currentQuestion.correctIndex = currentQuestion.options.length - 1;
                }
            }
        }

        // Push the last question
        if (currentQuestion) {
            categorizedQuestions[currentCategory].push(currentQuestion);
        }

        const total = categorizedQuestions.easy.length + categorizedQuestions.medium.length + categorizedQuestions.hard.length;
        console.log(`[QuizEngine] Successfully parsed ${total} questions from ${filePath}`);
        return categorizedQuestions;
    } catch (err) {
        console.error(`[QuizEngine] Error parsing quiz file ${filePath}:`, err.message);
        return null;
    }
}

function resolveQuizFilePath(filename) {
    const candidatePaths = [
        path.join(__dirname, '../quiz', filename),
        path.join(__dirname, '../../server/quiz', filename),
        path.join(__dirname, '../../client/quiz', filename),
        path.join(process.cwd(), 'server/quiz', filename),
        path.join(process.cwd(), 'quiz', filename),
        path.join(process.cwd(), 'client/quiz', filename)
    ];

    for (const p of candidatePaths) {
        if (fs.existsSync(p)) return p;
    }
    return null;
}

/**
 * Get 10 random questions: 4 easy, 3 medium, 3 hard
 */
function generateQuiz(league) {
    const l = league ? league.toLowerCase() : 'ipl';
    
    if (!quizCache[l]) {
        let filename = 'IPL_Quiz_Questions.txt';
        if (l === 'sa20') filename = 'SA20_Quiz_Questions.txt';
        else if (l === 'wpl') filename = 'WPL_Quiz_Questions.txt';

        const filePath = resolveQuizFilePath(filename);
        if (filePath) {
            const parsed = parseQuizFile(filePath);
            if (parsed) {
                quizCache[l] = parsed;
            }
        }

        if (!quizCache[l]) {
            // Fallback to IPL file
            const iplPath = resolveQuizFilePath('IPL_Quiz_Questions.txt');
            if (iplPath) {
                quizCache['ipl'] = parseQuizFile(iplPath);
                quizCache[l] = quizCache['ipl'];
            }
        }

        // Final fallback to built-in questions if file system is completely unavailable
        if (!quizCache[l]) {
            console.warn(`[QuizEngine] Using built-in fallback questions for ${l}`);
            quizCache[l] = FALLBACK_QUESTIONS;
        }
    }

    const pool = quizCache[l] || FALLBACK_QUESTIONS;

    // Helper to pick N random items
    const pickRandom = (arr, n) => {
        if (!arr || arr.length === 0) return [];
        const shuffled = [...arr].sort(() => 0.5 - Math.random());
        return shuffled.slice(0, n);
    };

    const easyQs = pickRandom(pool.easy.length > 0 ? pool.easy : FALLBACK_QUESTIONS.easy, 4).map(q => ({ ...q, difficulty: 'easy', points: 1 }));
    const medQs = pickRandom(pool.medium.length > 0 ? pool.medium : FALLBACK_QUESTIONS.medium, 3).map(q => ({ ...q, difficulty: 'medium', points: 2 }));
    const hardQs = pickRandom(pool.hard.length > 0 ? pool.hard : FALLBACK_QUESTIONS.hard, 3).map(q => ({ ...q, difficulty: 'hard', points: 3 }));

    // Combine and shuffle the 10 questions
    const finalQuiz = [...easyQs, ...medQs, ...hardQs].sort(() => 0.5 - Math.random());
    
    return finalQuiz.map((q, idx) => ({
        ...q,
        quizIndex: idx
    }));
}

module.exports = {
    generateQuiz
};
