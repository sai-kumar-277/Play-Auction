🌟 PlayAuction
The Ultimate Multi-League Cricket Auction Platform
Built for cricket enthusiasts, fantasy leagues, and friends who want to experience the thrill of a real-time mega auction.

📖 Why We Built This
Ever tried hosting a mock IPL auction with friends? It usually involves chaotic WhatsApp groups, messy Excel sheets, someone forgetting the budget, and constant arguments about who bid first. 
We wanted the real experience—the tension of the timer, the gavel slam, the strategic RTM (Right to Match) cards, and the thrill of outbidding your friends. 
PlayAuction was born from this exact frustration. We wanted to build a platform that doesn't just track bids, but recreates the entire auction room atmosphere. 

🎯 The problems we solved:
- 📱 Scattered tracking: No more messy spreadsheets or WhatsApp bids.
- 🕒 Real-time chaos: Synchronized bidding with live timers so everyone is on the same page.
- 🏏 League limitations: Why just IPL? We added WPL and SA20 support with accurate rules.
- 🤖 Missing players: Not enough friends? Configurable AI bots will bid against you.
- 📊 Post-auction blues: Once it's over, AI evaluates your squad and rates your performance!

🏗️ System Architecture
┌─────────────────────────────────────────────────────────────────┐
│                           CLIENT SIDE                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │             React Frontend (Vite + TailwindCSS)           │  │
│  │                                                           │  │
│  │  ┌────────────┐  ┌────────────┐  ┌─────────────────┐      │  │
│  │  │  Auction   │  │   Squad    │  │    Admin      │      │  │
│  │  │   Arena    │  │ Evaluation │  │  Dashboard    │      │  │
│  │  └────────────┘  └────────────┘  └─────────────────┘      │  │
│  └────────────────────────────┬──────────────────────────────┘  │
└───────────────────────────────┼─────────────────────────────────┘
                   HTTPS/REST API & Socket.io 
┌───────────────────────────────▼─────────────────────────────────┐
│                           SERVER SIDE                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                   Express.js Backend                      │  │
│  │                                                           │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐    │  │
│  │  │ Socket.io│  │ Auction  │  │  Auth &  │  │   AI    │    │  │
│  │  │  Engine  │  │  Routes  │  │  Admin   │  │ Services│    │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └─────────┘    │  │
│  └───────────────────────────────────────────────────────────┘  │
└───────────┬────────────────────┬────────────────────────────────┘
            │                    │
            ▼                    ▼
   ┌───────────────────┐  ┌─────────────────────────┐
   │    MongoDB Atlas  │  │     Google Gemini /     │
   │                   │  │       Groq AI           │
   │ ┌───────────────┐ │  │ ┌─────────────────────┐ │
   │ │  ipl, sa20,   │ │  │ │ AI Squad Evaluation │ │
   │ │ wpl databases │ │  │ │  & Quiz Generation  │ │
   │ └───────────────┘ │  │ └─────────────────────┘ │
   └───────────────────┘  └─────────────────────────┘

✨ Features Walkthrough

🏏 Multi-League Support
Experience the auction in your favorite format.
- Run auctions for IPL (15 franchises), WPL (5 franchises), or SA20 (6 franchises).
- League-specific rules, budgets, logos, and player pools.
- Pre-auction retentions and RTM logic for accurate squad building.

⚡ Real-Time Bidding Engine
The core of the excitement.
- Synchronized bidding across all clients via Socket.io.
- Live timer, automated bid increments, and real-time gavel slams.
- Immersive UI with voice announcements and background music.

🤖 AI-Powered Squad Evaluation
What happens after the hammer falls?
- Google Gemini + Groq/LangChain analyzes your final squad.
- Get AI-generated team ratings, insights, playing XI recommendations, and weaknesses.

🎓 Post-Auction Quiz Arena
Keep the fun going!
- Built-in cricket quiz generated dynamically based on the league.
- Live leaderboard to compete with your auction rivals.

🚀 Quick Start

Prerequisites
- Node.js (v20 or higher)
- npm or yarn
- MongoDB (local or Atlas) with separate databases: ipl, SA20, wpl
- Google Gemini API Key
- Groq API Key

Installation
1. Clone the repository
git clone 
cd PLAYAUCTION---A-MULTI-LEAGUE-AUCTION-GAME

2. Setup Backend
cd server
npm install
# Create .env file with:
PORT=5001
MONGODB_URI=your_mongodb_connection_string
GOOGLE_API_KEY=your_gemini_api_key
GEMINI_API_KEY=your_gemini_api_key
GEMINI_MODEL=gemini-2.5-flash
GROQ_API_KEY=your_groq_api_key
JWT_SECRET=your_jwt_secret_key
NODE_ENV=development
# Start backend server
npm run dev

3. Setup Frontend
cd ../client
npm install
# Create .env file with:
VITE_API_URL=http://localhost:5001
# Start frontend dev server
npm run dev

4. Open in browser
Navigate to http://localhost:5173

🛠️ Tech Stack

Frontend
| Technology | Purpose |
|---|---|
| React 19 + Vite 7 | Core UI framework and lightning-fast builds |
| TailwindCSS 3 | Utility-first styling for a sleek, immersive UI |
| Framer Motion | Smooth page and component animations |
| Socket.io-client| Real-time state sync with the auction room |
| html-to-image | Shareable squad result cards generation |
| canvas-confetti | Post-auction celebrations! |

Backend
| Technology | Purpose |
|---|---|
| Node.js + Express 5 | Robust REST API server |
| Socket.io 4 | Low-latency WebSockets for live bidding |
| MongoDB + Mongoose 9 | Multi-tenant database architecture (IPL, WPL, SA20) |
| Google Gemini API | Advanced squad analysis and quiz generation |
| Groq SDK + LangChain| Fast secondary AI inference pipeline |
| JWT + bcryptjs | Secure admin authentication |

📁 Project Structure
PLAYAUCTION/
├── client/                 # React frontend application
│   ├── public/             # Team logos, sounds, and background videos
│   ├── src/
│   │   ├── components/     # Reusable UI components (Bid panel, Player cards)
│   │   ├── context/        # Session, Socket, and Voice contexts
│   │   ├── pages/          # Auction Podium, Lobby, Results, Admin
│   │   └── utils/          # Bid logic, audio engine, etc.
│   └── package.json
└── server/                 # Express + Socket.io backend
    ├── config/             # DB connections
    ├── models/             # Mongoose schemas (Rooms, Players, Franchises)
    ├── routes/             # REST API routes
    ├── services/           # AI Queue, DB Batched Writers, Evaluation
    ├── socket/             # Core Socket.io event handlers
    ├── utils/              # Player caching and validation
    └── package.json

📖 How to Use

Starting an Auction
1. Admin creates a room (Public or Private) for a specific league.
2. Players join the room using the 6-digit room code.
3. Once all franchises are claimed, Admin starts the auction.
4. The bot brings up players one by one, and franchises bid in real-time.

Managing the Room
- Admin can pause the auction, skip players, or undo the last bid.
- RTM cards can be exercised when applicable.

After the Auction
- View the AI Evaluation of your squad.
- Play the interactive Quiz Arena.
- Generate and download your beautiful Team Squad Card to share on social media!

🔐 Security & Privacy
- JWT-based authentication for the Admin Dashboard.
- Encrypted MongoDB connections.
- Strict validation on Socket.io events to prevent unauthorized bidding or room tampering.
- Environment variables secure all AI and Database keys.

🗄️ Database Schema
ActiveRooms Collection
{ _id: ObjectId, roomCode: String, league: String, currentBid: Number, highestBidder: String, ... }

Players Collection (Per League DB)
{ _id: ObjectId, name: String, basePrice: Number, role: String, country: String, ... }

Franchises Collection (Per League DB)
{ _id: ObjectId, name: String, purseRemaining: Number, rtmCards: Number, squad: [PlayerId], ... }

🚀 Deployment
Frontend (Vercel)
1. Import repository to Vercel.
2. Set Build Command: `npm run build` and Output Directory: `dist`.
3. Add `VITE_API_URL` to environment variables.
4. Deploy! (Includes `vercel.json` for React Router support).

Backend (Render)
1. Create a New Web Service on Render.
2. Root Directory: `server`, Build: `npm install`, Start: `npm start`.
3. Add all required `.env` variables (MongoDB, Gemini, Groq, JWT).
4. Note: Server includes a self-ping mechanism to stay awake on Render's free tier.

🛣️ Roadmap
- [ ] Add more international leagues (BBL, PSL).
- [ ] Support for mega-auction retention rules (e.g., 6 retentions for IPL 2025).
- [ ] Player statistics integration (fetching real live stats).
- [ ] Voice bidding (bid using your microphone).

🤝 Contributing
We welcome contributions! 
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request


👥 Team
Built by cricket fans, for cricket fans.
Sai Kumar - Creator & Lead Developer (GitHub: @sai-kumar-277)

🙏 Acknowledgments
- Google Gemini & Groq for making our AI evaluations insanely fast and accurate.
- Socket.io for never dropping a bid.
- The cricket community for inspiring this project.

📞 Support
Found a bug? Have a feature request?
Issues: Open an issue on GitHub.
⭐ Star this repo if PlayAuction brought the thrill of the mega auction to your living room! ⭐
