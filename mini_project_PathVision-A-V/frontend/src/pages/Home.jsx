import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Logo from '../components/common/Logo';
import { useTheme } from '../context/ThemeContext';
import {
  FiActivity,
  FiCompass,
  FiMapPin,
  FiGitMerge,
  FiCalendar,
  FiBarChart2,
  FiShield,
  FiNavigation,
  FiClock,
  FiHeart,
} from 'react-icons/fi';
import { FaFacebookF, FaTwitter, FaInstagram, FaLinkedinIn, FaStar, FaBookOpen, FaPencilAlt, FaGraduationCap, FaRuler } from 'react-icons/fa';

const featureCards = [
  {
    title: 'AI-Powered Aptitude Assessment',
    description:
      'Comprehensive evaluation of logical reasoning, personality traits, academic strengths, and subject preferences through interactive quizzes.',
    icon: FiActivity,
  },
  {
    title: 'Smart Recommendations',
    description:
      'Get personalized stream, degree, and career suggestions with Decision Confidence Scores to help you choose with certainty.',
    icon: FiCompass,
    highlighted: true,
  },
  {
    title: 'Nearby College Directory',
    description:
      'Discover government colleges near you with detailed information about courses, eligibility, facilities, and admission processes.',
    icon: FiMapPin,
  },
  {
    title: 'Course-to-Career Mapping',
    description:
      'Visual pathways connecting degree programs to industries, job roles, government exams, and higher education opportunities.',
    icon: FiGitMerge,
  },
  {
    title: 'Timeline Tracker',
    description:
      'Never miss important deadlines for admissions, scholarships, and counseling schedules with smart notifications.',
    icon: FiCalendar,
  },
  {
    title: 'Skill Gap Analysis',
    description:
      'Identify gaps between your current abilities and career requirements with personalized learning resource recommendations.',
    icon: FiBarChart2,
  },
];

const steps = [
  {
    id: '01',
    title: 'Create Your Profile',
    text: 'Sign up and provide basic information about your academic background, interests, and career aspirations.',
    image:
      'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1200&q=80',
  },
  {
    id: '02',
    title: 'Take Assessments',
    text: 'Complete interactive aptitude tests and interest surveys designed to understand your strengths and preferences.',
    image:
      'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?auto=format&fit=crop&w=1200&q=80',
  },
  {
    id: '03',
    title: 'Get AI Recommendations',
    text: 'Receive personalized suggestions for streams, courses, and careers with confidence scores and detailed explanations.',
    image:
      'https://images.unsplash.com/photo-1521737711867-e3b97375f902?auto=format&fit=crop&w=1200&q=80',
  },
  {
    id: '04',
    title: 'Explore Opportunities',
    text: 'Discover nearby colleges, track deadlines, and access resources to bridge skill gaps and achieve your goals.',
    image:
      'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=80',
  },
];

const benefits = [
  {
    title: 'Reduce Uncertainty',
    text: 'Make confident decisions with AI-powered recommendations and Decision Confidence Scores.',
    icon: FiShield,
  },
  {
    title: 'Clear Career Pathways',
    text: 'Understand long-term outcomes with visual course-to-career mapping.',
    icon: FiNavigation,
  },
  {
    title: 'Access Government Colleges',
    text: 'Discover nearby institutions with complete details about courses and facilities.',
    icon: FiMapPin,
  },
  {
    title: 'Never Miss Deadlines',
    text: 'Stay informed about admissions, scholarships, and counseling schedules.',
    icon: FiClock,
  },
  {
    title: 'Bridge Skill Gaps',
    text: 'Get personalized learning resources to prepare for your dream career.',
    icon: FiBarChart2,
  },
  {
    title: 'Personalized Support',
    text: 'Receive guidance tailored to your unique aptitude, interests, and goals.',
    icon: FiHeart,
  },
];

const testimonials = [
  {
    name: 'Priya Sharma',
    role: 'Class 12 Student',
    image: 'https://randomuser.me/api/portraits/women/32.jpg',
    quote:
      'I was confused about choosing between engineering and medicine. The aptitude assessment and AI recommendations helped me understand my strengths better. Now I am confident about pursuing biotechnology.',
  },
  {
    name: 'Rahul Verma',
    role: 'Class 10 Graduate',
    image: 'https://randomuser.me/api/portraits/men/44.jpg',
    quote:
      'The college directory feature was a game-changer. I found three excellent government colleges near my hometown that I did not even know existed. The platform saved me so much time and effort.',
  },
  {
    name: 'Anjali Patel',
    role: 'Commerce Stream Student',
    image: 'https://randomuser.me/api/portraits/women/41.jpg',
    quote:
      'The course-to-career mapping showed me exactly what opportunities are available after my degree. The skill gap analysis helped me start preparing early.',
  },
  {
    name: 'Vikram Singh',
    role: 'Arts Stream Student',
    image: 'https://randomuser.me/api/portraits/men/65.jpg',
    quote:
      'As an arts student, I often felt limited in career options. This platform opened my eyes to many possibilities I had not considered. The personalized recommendations were spot on.',
  },
];

const Home = () => {
  const { theme } = useTheme();
  const isDark = theme === 'dark';
  const [parallax, setParallax] = useState({ x: 0, y: 0 });
  const darkHeroGradient = 'bg-[#011445]';

  const handleHeroMove = (e) => {
    if (window.innerWidth < 768) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;
    setParallax({ x, y });
  };

  const resetParallax = () => setParallax({ x: 0, y: 0 });

  return (
    <div className={`transition-colors duration-300 ${isDark ? `${darkHeroGradient} text-slate-100` : 'bg-[linear-gradient(180deg,#f6faf7_0%,#fcfffd_58%,#f7fcf8_100%)] text-slate-900'}`}>
      <section
        className={`relative min-h-screen overflow-hidden ${isDark ? 'bg-[#011445]' : 'bg-[linear-gradient(120deg,#495d95_0%,#5a6fb0_65%,#4a5f98_100%)]'}`}
        onMouseMove={handleHeroMove}
        onMouseLeave={resetParallax}
        style={{ '--mx': `${parallax.x * 26}px`, '--my': `${parallax.y * 22}px` }}
      >
        <div className="edu-float edu-book-1"><FaBookOpen /></div>
        <div className="edu-float edu-book-2"><FaBookOpen /></div>
        <div className="edu-float edu-pencil"><FaPencilAlt /></div>
        <div className="edu-float edu-cap"><FaGraduationCap /></div>
        <div className="edu-float edu-ruler"><FaRuler /></div>
        <div className="hero-blob hero-blob-1" />
        <div className="hero-blob hero-blob-2" />
        {isDark && (
          <>
            <div className="pointer-events-none absolute -left-20 top-24 h-72 w-72 rounded-full bg-sky-400/20 blur-3xl" />
            <div className="pointer-events-none absolute -right-12 bottom-10 h-80 w-80 rounded-full bg-violet-400/14 blur-3xl" />
          </>
        )}
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.12),transparent_42%),radial-gradient(circle_at_bottom_right,rgba(29,78,216,0.2),transparent_45%)]" />
        <div className="relative mx-auto flex min-h-screen max-w-7xl flex-col justify-center px-4 pb-12 pt-28 text-center sm:px-6 lg:px-8 lg:pt-32">
          <h1 className="reveal-up mx-auto max-w-5xl text-4xl font-extrabold leading-tight text-white sm:text-6xl lg:text-7xl" style={{fontSize:"60px"}}>
            Your Personalized Path to
            
            Academic and Career Success
          </h1>
          <p className="reveal-up delay-1 mx-auto mt-7 max-w-4xl text-xl leading-relaxed text-slate-100 sm:text-2xl">
            AI-powered guidance to help students make confident decisions about streams, courses, and careers based on
            aptitude, interests, and nearby educational opportunities
          </p>

          <div className="reveal-up delay-2 mt-9 flex flex-wrap items-center justify-center gap-4">
            <Link
              to="/register"
              className="rounded-xl bg-teal-500 px-10 py-4 text-xl font-semibold text-white shadow-lg transition hover:bg-teal-600"
            >
              Start Your Journey
            </Link>
            <a
              href="#features"
              className="rounded-xl border border-white/35 bg-white/10 px-10 py-4 text-xl font-semibold text-white transition hover:bg-white/20"
            >
              Learn More
            </a>
          </div>

          <div className="reveal-up delay-3 mt-14 grid gap-6 md:grid-cols-3">
            <div className="rounded-2xl border border-white/20 bg-white/10 p-8 backdrop-blur transition duration-300 hover:bg-[#4fcac1]/25">
              <p className="text-5xl font-extrabold text-teal-300">10,000+</p>
              <p className="mt-1 text-2xl font-medium text-white">Students Guided</p>
            </div>
            <div className="rounded-2xl border border-white/20 bg-white/10 p-8 backdrop-blur transition duration-300 hover:bg-[#4fcac1]/25">
              <p className="text-5xl font-extrabold text-teal-300">500+</p>
              <p className="mt-1 text-2xl font-medium text-white">Government Colleges</p>
            </div>
            <div className="rounded-2xl border border-white/20 bg-white/10 p-8 backdrop-blur transition duration-300 hover:bg-[#4fcac1]/25">
              <p className="text-5xl font-extrabold text-teal-300">95%</p>
              <p className="mt-1 text-2xl font-medium text-white">Satisfaction Rate</p>
            </div>
          </div>
        </div>
      </section>

      <section id="features" className="mx-auto w-full max-w-7xl px-4 pb-24 pt-16 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-4xl text-center">
          <h2 className={`reveal-up text-3xl font-extrabold tracking-tight sm:text-5xl ${isDark ? 'text-slate-100' : 'text-slate-900'}`}>Powerful Features for Your Success</h2>
          <p className={`reveal-up delay-1 mt-5 text-lg ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>Everything you need to make informed decisions about your academic and career future</p>
        </div>

        <div className="mt-14 grid gap-8 md:grid-cols-2 xl:grid-cols-3">
          {featureCards.map((item) => {
            const Icon = item.icon;
            return (
              <article
                key={item.title}
                style={{ animationDelay: `${0.1 * (item.highlighted ? 1 : 0)}s` }}
                className={`reveal-up group relative overflow-hidden rounded-2xl border p-9 pt-11 shadow-sm transition duration-300 hover:-translate-y-1 ${
                  isDark
                    ? `border-[#1f79aa]/70 ${item.highlighted ? 'bg-[#0d225c] shadow-md shadow-cyan-500/20' : 'bg-[#000d36]'} hover:border-purple-400/80 hover:bg-[#2d1659] hover:shadow-lg hover:shadow-purple-900/40`
                    : `bg-white/95 hover:bg-[#f3ebff] hover:border-purple-300 hover:shadow-lg hover:shadow-purple-200/60 ${item.highlighted ? 'border-emerald-200 shadow-md shadow-emerald-100/60' : 'border-slate-200 shadow-sm'}`
                }`}
              >
                <div
                  className={`absolute left-0 top-0 h-2 w-full transition-all duration-300 group-hover:h-2.5 ${
                    item.highlighted
                      ? 'bg-gradient-to-r from-teal-400 to-cyan-500 group-hover:from-purple-400 group-hover:to-fuchsia-500'
                      : 'bg-gradient-to-r from-cyan-300 to-sky-400 group-hover:from-purple-400 group-hover:to-fuchsia-500'
                  }`}
                />
                <div
                  className={`mb-7 inline-flex h-16 w-16 items-center justify-center rounded-xl ${
                    isDark
                      ? 'bg-[#0c1f52] text-emerald-400 ring-1 ring-emerald-400/40 transition duration-300 group-hover:bg-purple-500/20 group-hover:ring-purple-300/50'
                      : 'bg-[#ecfbf4] text-emerald-600 transition duration-300 group-hover:bg-purple-100'
                  }`}
                >
                  <Icon className="text-3xl" />
                </div>
                <h3 className={`text-2xl font-bold leading-tight ${isDark ? 'text-slate-100' : 'text-slate-900'}`}>{item.title}</h3>
                <p className={`mt-5 text-lg leading-relaxed ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>{item.description}</p>
              </article>
            );
          })}
        </div>
      </section>

      <section id="how-it-works" className={`${isDark ? darkHeroGradient : 'bg-[linear-gradient(180deg,#f8fcf9_0%,#f5f8f6_100%)]'} px-4 py-24 transition-colors duration-300 sm:px-6 lg:px-8`}>
        <div className="mx-auto w-full max-w-7xl">
          <div className="mx-auto max-w-4xl text-center">
            <h2 className={`reveal-up text-3xl font-extrabold sm:text-5xl ${isDark ? 'text-slate-100' : 'text-slate-900'}`}>How It Works</h2>
            <p className={`reveal-up delay-1 mt-5 text-lg ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>Four simple steps to unlock your personalized career guidance</p>
          </div>

          <div className="mt-16 space-y-20">
            {steps.map((step, index) => (
              <div key={step.id} className="grid items-center gap-12 lg:grid-cols-2 lg:gap-16">
                <div className={index % 2 === 1 ? 'order-2 lg:order-2' : 'order-2 lg:order-1'}>
                  <p className={`text-7xl font-extrabold ${isDark ? 'text-teal-400' : 'text-teal-800'}`}>{step.id}</p>
                  <h3 className={`mt-2 text-3xl font-bold ${isDark ? 'text-slate-100' : 'text-slate-900'}`}>{step.title}</h3>
                  <p className={`mt-4 max-w-xl text-lg leading-relaxed ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>{step.text}</p>
                </div>
                <div className={index % 2 === 1 ? 'order-1 lg:order-1' : 'order-1 lg:order-2'}>
                  <img src={step.image} alt={step.title} className="h-72 w-full rounded-2xl object-cover shadow-md sm:h-80 lg:h-96" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="benefits" className={`relative w-full px-4 py-24 transition-colors duration-300 sm:px-6 lg:px-8 ${isDark ? darkHeroGradient : 'bg-[linear-gradient(180deg,#edf7f1_0%,#f6faf8_48%,#ecf8f2_100%)]'}`}>
        {isDark && (
          <div className="pointer-events-none absolute left-1/2 top-0 h-44 w-2/3 -translate-x-1/2 rounded-full bg-sky-500/12 blur-3xl" />
        )}
        <div className="mx-auto w-full max-w-7xl">
          <div className="mx-auto max-w-4xl text-center">
            <h2 className={`reveal-up text-3xl font-extrabold sm:text-5xl ${isDark ? 'text-slate-100' : 'text-slate-900'}`}>Why Choose PathVision</h2>
            <p className={`reveal-up delay-1 mt-5 text-lg ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>Empowering students with technology-driven career guidance</p>
          </div>

          <div className="mt-16 grid gap-10 md:grid-cols-2 xl:grid-cols-3">
            {benefits.map((item) => {
              const Icon = item.icon;
              return (
                <article
                  key={item.title}
                  className={`reveal-up group relative overflow-hidden rounded-2xl border p-10 shadow-sm transition duration-300 hover:-translate-y-1 hover:shadow-lg ${
                    isDark ? 'border-[#1f79aa]/70 bg-[#000d36] hover:border-cyan-400/70 hover:bg-[#051a4c] shadow-cyan-950/30' : 'border-slate-200/70 bg-white/95 hover:bg-[#f3f7f4] shadow-sm'
                  }`}
                >
                  <div className="absolute left-0 top-0 h-1.5 w-full bg-gradient-to-r from-cyan-300 via-sky-300 to-blue-300 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
                  <div className="flex items-start gap-4">
                    <div className={`rounded-xl p-2.5 transition group-hover:bg-white ${isDark ? 'bg-[#0c1f52] text-cyan-300 ring-1 ring-cyan-400/40' : 'bg-[#ecfbf4] text-teal-600 ring-1 ring-emerald-100'}`}>
                      <Icon className="text-3xl" />
                    </div>
                    <div>
                      <h3 className={`text-2xl font-bold leading-tight ${isDark ? 'text-slate-100' : 'text-slate-900'}`}>{item.title}</h3>
                      <p className={`mt-3 text-lg leading-relaxed ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>{item.text}</p>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        </div>
      </section>

      <section id="testimonials" className={`${isDark ? darkHeroGradient : 'bg-[linear-gradient(180deg,#f9fcfa_0%,#f6faf8_100%)]'} px-4 py-24 transition-colors duration-300 sm:px-6 lg:px-8`}>
        <div className="mx-auto w-full max-w-7xl">
          <div className="mx-auto max-w-4xl text-center">
            <h2 className={`reveal-up text-3xl font-extrabold sm:text-5xl ${isDark ? 'text-slate-100' : 'text-slate-900'}`}>Student Success Stories</h2>
            <p className={`reveal-up delay-1 mt-5 text-lg ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>Hear from students who found their path with PathVision</p>
          </div>

          <div className="mt-14 grid gap-8 md:grid-cols-2">
            {testimonials.map((item) => (
              <article
                key={item.name}
                className={`reveal-up rounded-2xl border p-10 transition duration-300 hover:-translate-y-1 ${
                  isDark ? 'border-[#1f79aa]/70 bg-[#000d36] hover:border-cyan-400/70 hover:bg-[#051a4c] shadow-cyan-950/30' : 'border-slate-200/80 bg-white/95 hover:bg-[#fff8ee] shadow-sm'
                }`}
              >
                <div className="flex items-center gap-4">
                  <img src={item.image} alt={item.name} className="h-16 w-16 rounded-full object-cover" />
                  <div>
                    <h3 className={`text-2xl font-bold ${isDark ? 'text-slate-100' : 'text-slate-900'}`}>{item.name}</h3>
                    <p className={`text-lg ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>{item.role}</p>
                  </div>
                </div>
                <p className={`mt-6 text-lg italic leading-relaxed ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>"{item.quote}"</p>
                <div className="mt-6 flex items-center gap-1 text-amber-400">
                  <FaStar />
                  <FaStar />
                  <FaStar />
                  <FaStar />
                  <FaStar />
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className={`${isDark ? darkHeroGradient : 'bg-[linear-gradient(135deg,#11b78c_0%,#20bc9f_45%,#42b883_100%)]'} px-4 py-20 text-white transition-colors duration-300 sm:px-6`}>
        <div className="mx-auto max-w-5xl text-center">
          <h2 className={`text-3xl font-extrabold sm:text-5xl ${isDark ? 'text-slate-100' : 'text-white'}`}>Ready to Discover Your Perfect Career Path?</h2>
          <p className={`mx-auto mt-5 max-w-4xl text-lg leading-relaxed ${isDark ? 'text-slate-300' : 'text-teal-50'}`}>
            Join thousands of students who have found clarity and confidence in their academic and career decisions
          </p>
          <div className="mt-9 flex flex-wrap items-center justify-center gap-4">
            <Link to="/register" className={`rounded-xl px-10 py-4 text-xl font-semibold transition ${isDark ? 'bg-teal-500 text-white hover:bg-teal-600' : 'bg-white text-teal-600 hover:bg-slate-100'}`}>
              Get Started Free
            </Link>
            <Link
              to="/login"
              className={`rounded-xl border px-10 py-4 text-xl font-semibold transition ${isDark ? 'border-cyan-300/25 bg-[#0b1e56] text-slate-100 hover:bg-[#14306b]' : 'border-white/35 bg-white/10 text-white hover:bg-white/20'}`}
            >
              Sign In
            </Link>
          </div>
        </div>
      </section>

      <footer className={`${isDark ? darkHeroGradient : 'bg-[#081738]'} px-4 py-14 text-slate-300 transition-colors duration-300 sm:px-6`}>
        <div className={`mx-auto max-w-7xl border-b pb-10 ${isDark ? 'border-cyan-300/20' : 'border-slate-700'}`}>
          <div className="grid gap-10 md:grid-cols-4">
            <div>
              <div className="mb-4 flex items-center gap-3">
                <Logo width={34} height={34} className="text-teal-400" />
                <span className="text-3xl font-bold text-white">PathVision</span>
              </div>
              <p className="text-base leading-relaxed text-slate-400">
                AI-powered personalized career and education guidance for students
              </p>
            </div>

            <div>
              <h4 className="text-xl font-semibold text-white">Platform</h4>
              <ul className="mt-4 space-y-2 text-base">
                <li><a href="#features" className="hover:text-white">Features</a></li>
                <li><a href="#how-it-works" className="hover:text-white">How It Works</a></li>
                <li><a href="#benefits" className="hover:text-white">Benefits</a></li>
                <li><a href="#testimonials" className="hover:text-white">Testimonials</a></li>
              </ul>
            </div>

            <div>
              <h4 className="text-xl font-semibold text-white">Resources</h4>
              <ul className="mt-4 space-y-2 text-base">
                <li><a href="#" className="hover:text-white">College Directory</a></li>
                <li><a href="#" className="hover:text-white">Career Paths</a></li>
                <li><a href="#" className="hover:text-white">Blog</a></li>
                <li><a href="#" className="hover:text-white">FAQ</a></li>
              </ul>
            </div>

            <div>
              <h4 className="text-xl font-semibold text-white">Connect</h4>
              <div className="mt-5 flex gap-3">
                <a href="#" className="inline-flex h-12 w-12 items-center justify-center rounded-lg bg-[#0b1e56] text-slate-200 hover:bg-[#14306b]">
                  <FaFacebookF />
                </a>
                <a href="#" className="inline-flex h-12 w-12 items-center justify-center rounded-lg bg-[#0b1e56] text-slate-200 hover:bg-[#14306b]">
                  <FaTwitter />
                </a>
                <a href="#" className="inline-flex h-12 w-12 items-center justify-center rounded-lg bg-[#0b1e56] text-slate-200 hover:bg-[#14306b]">
                  <FaInstagram />
                </a>
                <a href="#" className="inline-flex h-12 w-12 items-center justify-center rounded-lg bg-[#0b1e56] text-slate-200 hover:bg-[#14306b]">
                  <FaLinkedinIn />
                </a>
              </div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Home;
