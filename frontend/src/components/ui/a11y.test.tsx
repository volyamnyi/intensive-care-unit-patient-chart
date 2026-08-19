import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Button } from './button';
import { Checkbox } from './checkbox';
import { RadioGroup, RadioGroupItem } from './radio-group';
import { Switch } from './switch';
import { Select, SelectTrigger, SelectValue } from './select';
import { Tabs, TabsList, TabsTrigger, TabsContent } from './tabs';
import { Input } from './input';

describe('ui accessibility contract', () => {
  it('exposes the expected interactive roles', () => {
    render(
      <div>
        <Button>Дія</Button>
        <Checkbox aria-label="Прапорець" />
        <Switch aria-label="Перемикач" />
        <RadioGroup aria-label="Група">
          <RadioGroupItem value="a" />
          <RadioGroupItem value="b" />
        </RadioGroup>
        <Select>
          <SelectTrigger aria-label="Вибір">
            <SelectValue placeholder="Оберіть" />
          </SelectTrigger>
        </Select>
        <Tabs defaultValue="one">
          <TabsList>
            <TabsTrigger value="one">Перша</TabsTrigger>
            <TabsTrigger value="two">Друга</TabsTrigger>
          </TabsList>
          <TabsContent value="one">Вміст</TabsContent>
        </Tabs>
        <Input aria-label="Поле вводу" />
      </div>,
    );
    expect(screen.getByRole('button', { name: 'Дія' })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Прапорець' })).toBeInTheDocument();
    expect(screen.getByRole('switch', { name: 'Перемикач' })).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: 'Група' })).toBeInTheDocument();
    expect(screen.getAllByRole('radio')).toHaveLength(2);
    expect(screen.getByRole('combobox', { name: 'Вибір' })).toBeInTheDocument();
    expect(screen.getByRole('tablist')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Перша' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Друга' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Поле вводу' })).toBeInTheDocument();
  });

  it('applies coarse-pointer touch targets to interactive primitives', () => {
    const { container } = render(
      <div>
        <Button>Кнопка</Button>
        <Checkbox aria-label="Прапорець" />
        <Switch aria-label="Перемикач" />
        <RadioGroup aria-label="Група">
          <RadioGroupItem value="a" />
        </RadioGroup>
        <Select>
          <SelectTrigger aria-label="Вибір">
            <SelectValue placeholder="Оберіть" />
          </SelectTrigger>
        </Select>
        <Tabs defaultValue="one">
          <TabsList>
            <TabsTrigger value="one">Перша</TabsTrigger>
          </TabsList>
          <TabsContent value="one">Вміст</TabsContent>
        </Tabs>
        <Input aria-label="Поле вводу" />
      </div>,
    );
    expect(container.querySelector('button')).toHaveClass('pointer-coarse:min-h-11');
    expect(container.querySelector('[data-slot="checkbox"]')).toHaveClass(
      'pointer-coarse:after:-inset-x-3.5',
    );
    expect(container.querySelector('[data-slot="switch"]')).toHaveClass(
      'pointer-coarse:after:-inset-y-3.5',
    );
    expect(container.querySelector('[data-slot="radio-group-item"]')).toHaveClass(
      'pointer-coarse:after:-inset-3.5',
    );
    expect(container.querySelector('[data-slot="select-trigger"]')).toHaveClass(
      'pointer-coarse:min-h-11',
    );
    expect(container.querySelector('[data-slot="tabs-list"]')).toHaveClass('pointer-coarse:min-h-11');
    expect(container.querySelector('[data-slot="input"]')).toHaveClass('pointer-coarse:min-h-11');
  });
});