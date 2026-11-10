import React from 'react';
import { PDFDocumentProxy } from 'pdfjs-dist';

interface SearchBarProps {
  isOpen: boolean;
  activeDoc: PDFDocumentProxy | null;
  numPages: number;
  onNavigateToPage: (page: number) => void;
  onClose: () => void;
}

interface MatchItem {
  pageNumber: number;
  matchIndex: number;
}

export const SearchBar: React.FC<SearchBarProps> = ({
  isOpen,
  activeDoc,
  numPages,
  onNavigateToPage,
  onClose,
}) => {
  const [query, setQuery] = React.useState('');
  const [searching, setSearching] = React.useState(false);
  const [matches, setMatches] = React.useState<MatchItem[]>([]);
  const [currentIndex, setCurrentIndex] = React.useState(-1);
  const [message, setMessage] = React.useState('');
  const inputRef = React.useRef<HTMLInputElement | null>(null);

  React.useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [isOpen]);

  const performSearch = async (searchTerm: string) => {
    const term = searchTerm.trim().toLowerCase();
    if (!term || !activeDoc) {
      setMatches([]);
      setCurrentIndex(-1);
      setMessage('');
      return;
    }

    setSearching(true);
    setMessage('Searching document...');

    const foundMatches: MatchItem[] = [];

    try {
      for (let p = 1; p <= numPages; p++) {
        const page = await activeDoc.getPage(p);
        const textContent = await page.getTextContent();
        const pageText = textContent.items
          .map((item: any) => item.str || '')
          .join(' ')
          .toLowerCase();

        let index = pageText.indexOf(term);
        let countOnPage = 0;
        while (index !== -1) {
          foundMatches.push({ pageNumber: p, matchIndex: countOnPage });
          countOnPage++;
          index = pageText.indexOf(term, index + term.length);
        }
      }

      setMatches(foundMatches);
      if (foundMatches.length > 0) {
        setCurrentIndex(0);
        setMessage(`${foundMatches.length} match${foundMatches.length === 1 ? '' : 'es'}`);
        onNavigateToPage(foundMatches[0].pageNumber);
      } else {
        setCurrentIndex(-1);
        setMessage('No matches found');
      }
    } catch (err) {
      console.error('Search failed:', err);
      setMessage('Search encountered an error');
    } finally {
      setSearching(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (e.shiftKey) {
        goToPrev();
      } else {
        if (matches.length > 0) {
          goToNext();
        } else {
          performSearch(query);
        }
      }
    } else if (e.key === 'Escape') {
      onClose();
    }
  };

  const goToNext = () => {
    if (matches.length === 0) return;
    const nextIdx = (currentIndex + 1) % matches.length;
    setCurrentIndex(nextIdx);
    onNavigateToPage(matches[nextIdx].pageNumber);
  };

  const goToPrev = () => {
    if (matches.length === 0) return;
    const prevIdx = (currentIndex - 1 + matches.length) % matches.length;
    setCurrentIndex(prevIdx);
    onNavigateToPage(matches[prevIdx].pageNumber);
  };

  if (!isOpen) return null;

  return (
    <div className="search-bar-floating">
      <div className="search-input-wrap">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          ref={inputRef}
          type="text"
          className="search-input"
          placeholder="Search in document..."
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            if (!e.target.value) {
              setMatches([]);
              setCurrentIndex(-1);
              setMessage('');
            }
          }}
          onKeyDown={handleKeyDown}
        />
      </div>

      <button
        className="search-action-btn"
        onClick={() => performSearch(query)}
        disabled={searching || !query.trim()}
        title="Find matches"
      >
        {searching ? '...' : 'Find'}
      </button>

      {message && (
        <span className="search-count-label">
          {matches.length > 0 ? `${currentIndex + 1}/${matches.length}` : message}
        </span>
      )}

      <div className="search-nav-buttons">
        <button
          className="search-nav-btn"
          onClick={goToPrev}
          disabled={matches.length <= 1}
          title="Previous match (Shift+Enter)"
        >
          ▲
        </button>
        <button
          className="search-nav-btn"
          onClick={goToNext}
          disabled={matches.length <= 1}
          title="Next match (Enter)"
        >
          ▼
        </button>
      </div>

      <button className="search-close-btn" onClick={onClose} title="Close search (Esc)">
        ×
      </button>
    </div>
  );
};
