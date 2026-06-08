export const metadata = {
  title: "Terms of Service | UFC Fight Predictor",
  description: "Terms of Service for UFC Fight Predictor",
};

export default function TermsOfService() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6 lg:px-8">
      <div className="rounded-2xl border border-white/10 bg-white/5 p-8 shadow-xl backdrop-blur-sm sm:p-12">
        <h1 className="mb-8 text-3xl font-bold tracking-tight text-white sm:text-4xl">Terms of Service</h1>
        
        <div className="space-y-8 text-white/70">
          <section>
            <p className="mb-4">Last updated: {new Date().toLocaleDateString()}</p>
            <p>
              Please read these Terms of Service ("Terms", "Terms of Service") carefully before using the UFC Fight Predictor 
              website (the "Service") operated by UFC Fight Predictor ("us", "we", or "our").
            </p>
            <p className="mt-4">
              Your access to and use of the Service is conditioned on your acceptance of and compliance with these Terms. 
              These Terms apply to all visitors, users and others who access or use the Service.
            </p>
          </section>

          <section>
            <h2 className="mb-4 text-xl font-semibold text-white">1. Accounts</h2>
            <p>
              When you create an account with us, you must provide us information that is accurate, complete, and current at all times. 
              Failure to do so constitutes a breach of the Terms, which may result in immediate termination of your account on our Service.
            </p>
            <p className="mt-4">
              You are responsible for safeguarding the password that you use to access the Service and for any activities or actions under your password.
            </p>
          </section>

          <section>
            <h2 className="mb-4 text-xl font-semibold text-white">2. User Conduct & Forum Rules</h2>
            <p className="mb-2">You agree not to engage in any of the following activities when using the Service, including our community forums:</p>
            <ul className="list-disc pl-5 space-y-2">
              <li>Attempting to manipulate, exploit, or cheat the prediction system or leaderboard.</li>
              <li>Posting or transmitting content that is illegal, abusive, harassing, defamatory, or hateful.</li>
              <li>Spamming, posting unauthorized advertising, or disrupting the normal flow of dialogue in the forums.</li>
              <li>Using the Service for any unlawful purpose or in violation of any local, state, national, or international law.</li>
              <li>Impersonating any person or entity, or falsely stating or otherwise misrepresenting your affiliation with a person or entity.</li>
              <li>Interfering with or disrupting the Service or servers or networks connected to the Service.</li>
            </ul>
            <p className="mt-4">
              We reserve the right, but have no obligation, to monitor and moderate the forums. We may remove any content or terminate accounts that violate these guidelines at our sole discretion.
            </p>
          </section>

          <section>
            <h2 className="mb-4 text-xl font-semibold text-white">3. Intellectual Property</h2>
            <p>
              The Service and its original content, features and functionality are and will remain the exclusive property of 
              UFC Fight Predictor and its licensors. The Service is protected by copyright, trademark, and other laws.
              UFC Fight Predictor is not affiliated with the Ultimate Fighting Championship (UFC). All UFC trademarks, 
              logos, and brand names are the property of their respective owners.
            </p>
          </section>

          <section>
            <h2 className="mb-4 text-xl font-semibold text-white">4. Limitation of Liability</h2>
            <p>
              In no event shall UFC Fight Predictor, nor its directors, employees, partners, agents, suppliers, or affiliates, 
              be liable for any indirect, incidental, special, consequential or punitive damages, including without limitation, 
              loss of profits, data, use, goodwill, or other intangible losses, resulting from your access to or use of or 
              inability to access or use the Service.
            </p>
          </section>

          <section>
            <h2 className="mb-4 text-xl font-semibold text-white">5. Changes</h2>
            <p>
              We reserve the right, at our sole discretion, to modify or replace these Terms at any time. What constitutes a 
              material change will be determined at our sole discretion. By continuing to access or use our Service after those 
              revisions become effective, you agree to be bound by the revised terms.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
