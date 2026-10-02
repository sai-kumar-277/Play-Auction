# 🌟 PlayAuction

> **The Ultimate Multi-League Cricket Auction Platform**

Built for cricket enthusiasts, fantasy leagues, and friends who want to experience the thrill of a real-time mega auction.

---

## 📖 Why We Built This

Ever tried hosting a mock IPL auction with friends? It usually involves chaotic WhatsApp groups, messy Excel sheets, someone forgetting the budget, and constant arguments about who bid first. 

We wanted the real experience—the tension of the timer, the gavel slam, the strategic RTM (Right to Match) cards, and the thrill of outbidding your friends. 

**PlayAuction was born from this exact frustration.** We wanted to build a platform that doesn't just track bids, but recreates the entire auction room atmosphere.

The problems we solved:
- 📱 **Scattered tracking**: No more messy spreadsheets or WhatsApp bids
- 🕒 **Real-time chaos**: Synchronized bidding with live timers so everyone is on the same page
- 🏏 **League limitations**: Why just IPL? We added WPL and SA20 support with accurate rules
- 🤖 **Missing players**: Not enough friends? Configurable AI bots will bid against you
- 📊 **Post-auction blues**: Once it's over, AI evaluates your squad and rates your performance!

---

## 🏗️ System Architecture

```text
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT SIDE                              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  React Frontend (Vite + TailwindCSS)                      │   │
│  │  ┌────────────┐  ┌────────────┐  ┌─────────────────┐     │   │
│  │  │  Auction   │  │   Squad    │  │     Admin       │     │   │
│  │  │   Arena    │  │ Evaluation │  │   Dashboard     │     │   │
│  │  └────────────┘  └────────────┘  └─────────────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTPS/REST API & Socket.io
┌───────────────────────────▼─────────────────────────────────────┐
│                        SERVER SIDE                               │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Express.js Backend                                       │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐  │   │
│  │  │ Socket.io│  │ Auction  │  │  Auth &  │  │   AI    │  │   │
│  │  │  Engine  │  │  Routes  │  │  Admin   │  │ Services│  │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └─────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────────┬────────────────────┬────────────────────────────────┘
            │                    │
            ▼                    ▼
┌───────────────────┐  ┌─────────────────────────┐
│   MongoDB Atlas   │  │  Google Gemini / Groq   │
│  ┌─────────────┐  │  │  ┌──────────────────┐  │
│  │ ipl Database│  │  │  │  Squad Analysis  │  │
│  │ wpl Database│  │  │  │  Quiz Generation │  │
│  │sa20 Database│  │  │  └──────────────────┘  │
│  └─────────────┘  │  └─────────────────────────┘
└───────────────────┘
```

---

## ✨ Features Walkthrough

### 🏏 Multi-League Support
**Experience the auction in your favorite format**

Run your auctions exactly how you want:
- **IPL, WPL, or SA20**: Choose your league (15, 5, or 6 franchises)
- **League-specific rules**: Accurate budgets, logos, and player pools
- **Pre-auction retentions**: Validate RTM logic for accurate squad building

*Tech:* MongoDB multi-tenant database architecture separating `ipl`, `wpl`, and `sa20` collections.

### ⚡ Real-Time Bidding Engine
**The Core of the Excitement**

Synchronized bidding across all clients:
- **Live timer**: Automated bid increments and real-time gavel slams
- **AI Bots**: Configurable bot players to bid when humans are unavailable
- **Immersive UI**: Voice announcements, background music, and smooth animations

*Tech:* Socket.io engine for low-latency WebSockets and synchronized state machines.

### 🤖 AI-Powered Squad Evaluation
**What Happens After the Hammer Falls?**

Get a detailed breakdown of your auction performance:
- **Team Ratings**: AI analyzes your final squad balance
- **Playing XI**: Get AI-generated playing XI recommendations
- **Insights & Weaknesses**: Learn where your squad is lacking

*Tech:* Google Gemini 2.5 Flash + Groq/LangChain for fast and accurate secondary AI inference.

### 🎓 Post-Auction Quiz Arena
**Keep the Fun Going!**

- **Dynamic Quizzes**: Built-in cricket quiz generated based on the league
- **Live Leaderboard**: Compete with your auction rivals even after the bidding stops

*Tech:* Dynamic question parsing and real-time Socket.io leaderboard updates.

---

## 🚀 Quick Start

### Prerequisites
```bash
# Required installed software:
- Node.js (v20 or higher)
- npm or yarn
- MongoDB (local or Atlas account)
- Google Gemini API key
- Groq API key
```

### Installation

**1. Clone the repository**
```bash
git clone https://github.com/Venkat262005/PLAYAUCTION---A-MULTI-LEAGUE-AUCTION-GAME.git
cd PLAYAUCTION---A-MULTI-LEAGUE-AUCTION-GAME
```

**2. Setup Backend**
```bash
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
```

**3. Setup Frontend**
```bash
cd ../client
npm install

# Create .env file with:
VITE_API_URL=http://localhost:5001

# Start frontend dev server
npm run dev
```

**4. Open in browser**
```
Navigate to http://localhost:5173
```

---

## 🛠️ Tech Stack

### Frontend
| Technology | Purpose |
|------------|---------|
| **React 19** | Core UI framework |
| **Vite 7** | Lightning-fast build tool |
| **TailwindCSS 3** | Utility-first styling for a sleek, immersive UI |
| **Framer Motion** | Smooth page and component animations |
| **Socket.io-client** | Real-time state sync with the auction room |
| **html-to-image** | Shareable squad result cards generation |
| **canvas-confetti** | Post-auction celebrations! |

### Backend
| Technology | Purpose |
|------------|---------|
| **Node.js** | JavaScript runtime |
| **Express 5** | Robust REST API server |
| **Socket.io 4** | Low-latency WebSockets for live bidding |
| **MongoDB / Mongoose 9** | Multi-tenant database architecture |
| **Google Gemini API** | Advanced squad analysis and quiz generation |
| **Groq SDK + LangChain** | Fast secondary AI inference pipeline |
| **JWT + bcryptjs** | Secure admin authentication |

---

## 📁 Project Structure

```text
PLAYAUCTION/
│
├── client/                        # React frontend application
│   ├── public/                    # Team logos, sounds, and bg videos
│   ├── src/
│   │   ├── components/            # Reusable UI components
│   │   ├── context/               # Session, Socket, and Voice contexts
│   │   ├── pages/                 # Route pages (Arena, Lobby, Admin)
│   │   └── utils/                 # Bid logic, audio engine
│   └── package.json
│
├── server/                        # Express + Socket.io backend
│   ├── config/                    # DB connections
│   ├── models/                    # Mongoose schemas (Rooms, Players)
│   ├── routes/                    # API route handlers
│   ├── services/                  # AI Queue, DB Batched Writers
│   ├── socket/                    # Core Socket.io event handlers
│   ├── utils/                     # Player caching and validation
│   └── package.json
│
└── README.md                      # You are here!
```

---

## 📖 How to Use

### Starting an Auction
1. **Admin creates a room** (Public or Private) for a specific league.
2. **Players join the room** using the 6-digit room code.
3. Once all franchises are claimed, Admin starts the auction.
4. The bot brings up players one by one, and franchises bid in real-time.

### Managing the Room
- Admin can **pause the auction**, **skip players**, or **undo the last bid**.
- **RTM cards** can be exercised when applicable.

### After the Auction
- View the **AI Evaluation** of your squad.
- Play the interactive **Quiz Arena**.
- Generate and download your beautiful **Team Squad Card** to share on social media!

---

## 🔐 Security & Privacy

- **JWT-based authentication** for the Admin Dashboard
- **Encrypted MongoDB connections** using TLS
- **Strict Socket.io validation** to prevent unauthorized bidding or room tampering
- **Environment variables** protect API keys and database secrets

---

## 🗄️ Database Schema

### ActiveRooms Collection
```javascript
{
  _id: ObjectId,
  roomCode: String,
  league: String,
  currentBid: Number,
  highestBidder: String
  // ...
}
```

### Players Collection (Per League)
```javascript
{
  _id: ObjectId,
  name: String,
  basePrice: Number,
  role: String,
  country: String
  // ...
}
```

### Franchises Collection (Per League)
```javascript
{
  _id: ObjectId,
  name: String,
  purseRemaining: Number,
  rtmCards: Number,
  squad: [PlayerId]
  // ...
}
```

---

## 🚀 Deployment

### Frontend (Vercel)
1. **Connect GitHub Repository** in Vercel
2. **Build Settings**: Command: `npm run build`, Output: `dist`
3. **Environment Variables**: Add `VITE_API_URL`
4. **Deploy**: Includes `vercel.json` for React Router support

### Backend (Render)
1. **Create Web Service** on Render
2. **Service Settings**: Root: `server`, Build: `npm install`, Start: `npm start`
3. **Environment Variables**: Add MongoDB, Gemini, Groq, and JWT secrets
4. *Note:* Server includes a self-ping mechanism to stay awake on Render's free tier.

---

## 🛣️ Roadmap

- [ ] **International Leagues** (BBL, PSL support)
- [ ] **Mega-Auction Retention Rules** (e.g., 6 retentions for IPL 2025)
- [ ] **Live Player Stats** integration
- [ ] **Voice Bidding** (bid using your microphone!)

---

## 🤝 Contributing

We welcome contributions! Here's how:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **ISC License**.

---

## 👥 Team

Built by cricket fans, for cricket fans.

### **Venkat**
🔧 **Focus**: Creator, Lead Developer, Architect  
📧 **GitHub**: [@Venkat262005](https://github.com/Venkat262005)  
💡 *"Bringing the intensity of the auction room to your living room."*

---

## 🙏 Acknowledgments

- **Google Gemini & Groq** for making our AI evaluations insanely fast and accurate.
- **Socket.io** for never dropping a bid.
- The cricket community for inspiring this project.

---

## 📞 Support

Found a bug? Have a feature request? Want to say hi?

- **Issues**: Open an issue on GitHub
- **Discussions**: Use GitHub Discussions for questions

---

<div align="center">

**⭐ Star this repo if PlayAuction brought the thrill of the mega auction to your living room! ⭐**

*Made with 🏏 and 💻*

</div>
