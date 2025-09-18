import React, { useEffect, useMemo, useState } from 'react';
import { BoardsAPI, type BoardSummary, type BoardCreateRequest, type BoardUpdateRequest } from './api/boards';
import { SearchBar } from './SearchBar';
import { Pagination } from './Pagination';
import { Button } from './Button';
import { BoardTable, type TableColumn } from './BoardTable';
import { useNavigate } from 'react-router-dom';

/* =========================
   타입 모음: 쿼리 · 상태 · 핸들러 · 컬럼
   ========================= */

// 목록 조회 쿼리(BoardsAPI.list의 params와 동일 스펙 유지)
type BoardListQuery = { boardTitle?: string };

//타입
type BoardListState = {
  rows: BoardSummary[];
  keyword: string;
  loading: boolean;
  error: string | null;
  pageIndex: number;
  pageSize: number;
  showCreate: boolean;
  editTarget: BoardSummary | null;
};

// 컬럼 제네릭 고정
type BoardColumns = TableColumn<BoardSummary>[];

// 컬럼에서 사용할 액션(이벤트) 핸들러
type BoardColumnHandlers = {
  onEdit: (b: BoardSummary) => void;
  onView: (b: BoardSummary) => void;
};

// API 시그니처로부터 인자 타입 자동 추출(중복 제거)
type CreateBody = Parameters<typeof BoardsAPI.create>[0];     // BoardCreateRequest
type UpdateArgs = Parameters<typeof BoardsAPI.update>;         // [boardId, memberId, body]
type RemoveArgs = Parameters<typeof BoardsAPI.remove>;         // [boardId, memberId]
type BoardId   = UpdateArgs[0];
type MemberId  = UpdateArgs[1];
type UpdateBody = UpdateArgs[2];

/* =========================
   컬럼 팩토리
   ========================= */

export const createBoardColumns = (h: BoardColumnHandlers): BoardColumns => [
  { header: '번호',        render: (b) => b.boardId },
  { header: '게시판 제목',  render: (b) => <a href={`/CMS/boards/${b.boardId}/posts`}>{b.boardTitle}</a> },
  { header: '게시판 편집',  render: (b) => <button onClick={() => h.onEdit(b)}>편집</button> },
  { header: '게시글 조회',  render: (b) => <button onClick={() => h.onView(b)}>조회</button> },
  { header: '이용 가능',    render: (b) => (b.boardUse === 'Y' ? 'Y' : 'N') },
  { header: '등록일',      render: (b) => b.regDate ?? '-' },
  { header: '수정일',      render: (b) => b.modDate ?? '-' },
];

/* =========================
   컴포넌트 본문
   ========================= */

export default function CMSBoard() {
  // 개별 상태(useReducer로 합칠 수도 있으나 기존 구조 유지)
  const [rows, setRows] = useState<BoardSummary[]>([]);
  const [keyword, setKeyword] = useState<BoardListState['keyword']>('');
  const [loading, setLoading] = useState<BoardListState['loading']>(false);
  const [error, setError] = useState<BoardListState['error']>(null);
  const [pageIndex, setPageIndex] = useState<BoardListState['pageIndex']>(0);
  const [pageSize] = useState<BoardListState['pageSize']>(10);
  const [showCreate, setShowCreate] = useState<BoardListState['showCreate']>(false);
  const [editTarget, setEditTarget] = useState<BoardListState['editTarget']>(null);

  // 목록 조회 (AbortController 지원)
  const fetchList = async (kw?: BoardListQuery['boardTitle'], signal?: AbortSignal) => {
    setLoading(true);
    setError(null);
    try {
      const data = await BoardsAPI.list({ boardTitle: kw || undefined });
      if (signal?.aborted) return;
      setRows(data);
      setPageIndex(0);
    } catch (e: any) {
      if (e?.name !== 'AbortError') setError('목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 최초 로드
  useEffect(() => {
    const ac = new AbortController();
    fetchList(undefined, ac.signal);
    return () => ac.abort();
  }, []);

  // 페이지네이션(클라이언트 슬라이싱)
  const paged = useMemo(() => {
    if (!Array.isArray(rows)) return [];
    const start = pageIndex * pageSize;
    return rows.slice(start, start + pageSize);
  }, [rows, pageIndex, pageSize]);

  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));

  // 생성/수정/삭제(유틸 타입으로 시그니처 동기화)
  const onCreate = async (form: CreateBody) => {
    await BoardsAPI.create(form);
    setShowCreate(false);
    await fetchList(keyword);
  };

  const onUpdate = async (boardId: BoardId, memberId: MemberId, body: UpdateBody) => {
    await BoardsAPI.update(boardId, memberId, body);
    await fetchList(keyword);
  };

  const onRemove = async (boardId: RemoveArgs[0], memberId: RemoveArgs[1]) => {
    await BoardsAPI.remove(boardId, memberId);
    await fetchList(keyword);
  };

  const handleSave = () => {
    alert('저장 버튼 클릭됨! 실제 저장 로직 연결 필요');
  };

  // 컬럼 정의: 핸들러 주입 (navigate 의존성 반영)
  const navigate = useNavigate();
  const columns = useMemo(
    () =>
      createBoardColumns({
        onEdit: (b) => setEditTarget(b),
        onView: (b) => {
          navigate(`/CMS/boards/${b.boardId}/posts`);
        },
      }),
    [navigate]
  );

  return (
    <div>
      <h2>게시판 관리</h2>

      <SearchBar
        keyword={keyword}
        onKeywordChange={setKeyword}
        onSearch={() => {
          const ac = new AbortController();
          fetchList(keyword, ac.signal);
        }}
      />

      <Button label="저장" onClick={handleSave} />

      {loading && <p>로딩 중...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}

      <BoardTable data={paged} columns={columns} keyField="boardId" />

      <Pagination pageIndex={pageIndex} totalPages={totalPages} onPageChange={setPageIndex} />
    </div>
  );
}
