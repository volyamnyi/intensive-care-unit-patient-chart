import { Tooltip, TooltipTrigger, TooltipContent } from '@/components/ui/tooltip';
import { Sun, Moon } from 'lucide-react';
import { useThemeMode } from '../../styles/ThemeContext';

export default function ThemeToggle() {
  const { toggleTheme, mode } = useThemeMode();
  return (
    <Tooltip>
      <TooltipTrigger
        onClick={toggleTheme}
        aria-label="Переключити тему"
        className="inline-flex size-8 items-center justify-center rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground hover:text-primary hover:bg-primary/10"
      >
        {mode === 'dark' ? <Sun className="size-5" /> : <Moon className="size-5" />}
      </TooltipTrigger>
      <TooltipContent>
        {mode === 'dark' ? 'Світла тема' : 'Темна тема'}
      </TooltipContent>
    </Tooltip>
  );
}
