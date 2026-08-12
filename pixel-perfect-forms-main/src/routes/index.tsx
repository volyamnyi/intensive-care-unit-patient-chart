import { createFileRoute } from "@tanstack/react-router";
import form1 from "@/assets/form1.png.asset.json";
import form2 from "@/assets/form2.png.asset.json";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Upper Limb Measurement Form" },
      {
        name: "description",
        content:
          "Measurement form for upper limb casting: chest circumference, acromion, axilla, epicondyle, styloid and thumb tip values.",
      },
      { property: "og:title", content: "Upper Limb Measurement Form" },
      {
        property: "og:description",
        content:
          "Enter arm measurements directly on the anatomical measurement diagrams.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Index,
});

type Field = { top: number; left: number; w: number; h: number };

const CIRCLE = "circle" as const;

function Circle({ top, left, w, h, name }: Field & { name: string }) {
  return (
    <input
      type="text"
      name={name}
      aria-label={name}
      className="measure-field measure-circle"
      style={{ top, left, width: w, height: h }}
    />
  );
}

function Square({ top, left, w, h, name }: Field & { name: string }) {
  return (
    <input
      type="text"
      name={name}
      aria-label={name}
      className="measure-field measure-square"
      style={{ top, left, width: w, height: h }}
    />
  );
}

function Index() {
  return (
    <main className="min-h-screen bg-background py-10">
      <h1 className="sr-only">Upper limb measurement form</h1>

      <form className="flex flex-col items-center gap-10">
        {/* Form 1 */}
        <section
          className="measure-sheet"
          style={{ width: 295, height: 523 }}
          aria-label="Full arm measurement diagram"
        >
          <img src={form1.url} alt="" width={295} height={523} draggable={false} />

          <Circle top={4} left={23} w={51} h={50} name="Chest circumference" />
          <Circle top={111} left={81} w={52} h={50} name="Axilla circumference" />
          <Circle top={242} left={81} w={52} h={50} name="Epicondyle circumference" />
          <Circle top={336} left={81} w={52} h={50} name="Forearm circumference" />
          <Circle top={400} left={81} w={52} h={50} name="Styloid circumference" />

          <Square top={178} left={23} w={50} h={48} name="Acromion to epicondyle" />
          <Square top={336} left={23} w={50} h={48} name="Epicondyle to thumb tip" />
          <Square top={178} left={223} w={50} h={48} name="Axilla to epicondyle" />
          <Square top={336} left={223} w={50} h={48} name="Epicondyle to styloid" />
          <Square top={437} left={223} w={50} h={48} name="Styloid to thumb tip" />
        </section>

        {/* Form 2 */}
        <section
          className="measure-sheet"
          style={{ width: 343, height: 264 }}
          aria-label="Epicondyle detail measurement diagram"
        >
          <img src={form2.url} alt="" width={343} height={264} draggable={false} />

          <Square top={61} left={66} w={56} h={56} name="Forearm width" />
          <Square top={61} left={225} w={57} h={56} name="Cast height at epicondyle" />

          <Circle top={4} left={280} w={57} h={57} name="Proximal circumference" />
          <Circle top={110} left={280} w={57} h={58} name="Distal circumference" />

          <Circle top={204} left={6} w={58} h={59} name="Distal forearm circumference" />
          <Circle top={205} left={64} w={58} h={58} name="Mid forearm circumference" />
          <Circle top={204} left={125} w={58} h={59} name="Proximal forearm circumference" />
        </section>
      </form>
    </main>
  );
}

export { CIRCLE };
