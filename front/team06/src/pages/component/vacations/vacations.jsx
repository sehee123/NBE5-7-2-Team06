import React, {useState, useEffect,useRef} from "react";
import {Search, Save, ChevronLeft, ChevronRight, Loader2} from "lucide-react";
import axios from '../../../api/axiosInstance';

// 실제 휴가 유형 맵핑
const vacationTypeMap = {
    "01": "연차",
    "02": "포상 휴가",
    "03": "공가",
    "04": "경조사 휴가"
};

const vacationTypes = Object.keys(vacationTypeMap);

export default function Vacations() {
    const [vacationData, setVacationData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchName, setSearchName] = useState("");
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const pageSize = 10;

    const requestedRef = useRef(false);
    useEffect(() => {
        if (requestedRef.current) return;
        requestedRef.current = true;
        fetchVacationData(currentPage, searchName);
    }, [currentPage]);

    const fetchVacationData = async (page, name) => {
        setLoading(true);
        try {
            const response = await axios.get('/admin/vacations/infos', {
                params: {
                    name: name,
                    page: page,
                    size: pageSize
                }
            });

            const data = response.data;
            setVacationData(data.content || []);
            setTotalPages(data.totalPages || 1);
        } catch (e) {
            if (e.response?.status === 401) {
               alert('접근 권한이 없습니다.');
            }else{
                alert('데이터 조회 실패');
            }
        } finally {
            setLoading(false);
            requestedRef.current = false;
        }
    };

    const handleCountChange = (employeeId, vacationId, newValue) => {
        setVacationData(prev =>
            prev.map(emp =>
                emp.id === employeeId
                    ? {
                        ...emp,
                        vacationInfos: emp.vacationInfos.map(v =>
                            v.id === vacationId
                                ? {...v, totalCount: parseFloat(newValue)}
                                : v
                        ),
                    }
                    : emp
            )
        );
    };

    const saveEmployee = async (employee) => {
        try {
            const response = await axios.patch('/admin/vacations/infos', {
                requests: [
                    {
                        memberId: employee.id,
                        vacations: employee.vacationInfos.map(v => ({
                            id: v.id,
                            totalCount: v.totalCount,
                            type: v.vacationType || v.type,
                            version: v.version,
                        })),
                    },
                ],
            });

            alert(`${employee.name} 저장 완료`);
        } catch (e) {
            const res = e.response?.data;

            if (res?.codeName === 'CONFLICT_VERSION') {
                alert('변경중 문제가 발생하였습니다. 다시 요청해 주세요');
                return;
            }

            if (res?.status === 400) {
                alert('잘못된 입력입니다.');
            } else {
                alert('문제가 발생하였습니다');
            }
        } finally {
            fetchVacationData(currentPage, searchName);
        }
    };

    const  throwError = async (response) => {
        let errorData = {};
        try {
            errorData = await response.json(); // 서버 에러 메시지 파싱
        } catch {
            errorData.message = '알 수 없는 에러';
        }

        const error = new Error(errorData.message || '저장 실패');
        error.codeName = errorData.codeName || null;
        error.status = errorData.status || response.status;
        error.validationErrors = errorData.errors || null;
        throw error;
    }

    const handleSaveAll = async () => {
        try {
            const requests = vacationData.map(emp => ({
                memberId: emp.id,
                vacations: emp.vacationInfos.map(v => ({
                    id: v.id,
                    totalCount: v.totalCount,
                    type: v.vacationType || v.type,
                    version: v.version,
                })),
            }));

            const response = await axios.patch('/admin/vacations/infos', { requests });

            alert('전체 저장 완료');
        } catch (e) {
            const res = e.response?.data;

            if (res?.codeName === 'CONFLICT_VERSION') {
                alert('변경중 문제가 발생하였습니다. 다시 요청해 주세요');
                return;
            }

            if (res?.status === 400) {
                alert('잘못된 입력입니다.');
            } else {
                alert('문제가 발생하였습니다');
            }
        } finally {
            fetchVacationData(currentPage, searchName);
        }
    };

    const handlePageChange = page => {
        if (page >= 0 && page < totalPages) {
            setCurrentPage(page);
        }
    };

    const handleSearch = () => {
        setCurrentPage(0);
        fetchVacationData(0, searchName);
    };

    return (
        <div className="p-6 bg-gray-50 min-h-screen">
            <div className="max-w-7xl mx-auto">
                <div className="mb-6">
                    <h1 className="text-2xl font-bold text-gray-900">사원별 휴가 정보 관리</h1>
                    <p className="text-gray-600 mt-2">
                        사원별로 휴가 정보를 관리 할 수 있습니다.
                    </p>
                </div>
            </div>
            {/* 필터 영역 */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-6">
                <h2 className="text-lg font-semibold text-gray-800 mb-4">
                    검색 필터
                </h2>
                <div className="space-y-4">
                    <div className="flex flex-wrap items-center justify-between gap-4">
                        <div className="relative">
                            <div className="flex items-center">
                                <div className="relative">
                                    <input
                                        type="text"
                                        value={searchName}
                                        placeholder="이름 검색"
                                        onChange={e => setSearchName(e.target.value)}
                                        onKeyDown={e => e.key === "Enter" && handleSearch()}
                                        className="pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 w-64 transition-all"
                                    />
                                    <div className="absolute left-3 top-2.5 text-gray-400">
                                        <Search size={18}/>
                                    </div>
                                </div>
                                <button
                                    onClick={handleSearch}
                                    className="ml-2 bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg transition-colors duration-200 font-medium flex items-center gap-1"
                                >
                                    검색
                                </button>
                            </div>
                        </div>

                        <button
                            onClick={handleSaveAll}
                            className="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2 rounded-lg shadow transition-colors duration-200 font-medium flex items-center gap-2"
                        >
                            <Save size={18}/>
                            전체 저장
                        </button>
                    </div>
                </div>
            </div>

            {/* 테이블 */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                {loading ? (
                    <div className="flex justify-center items-center py-12">
                        <Loader2 className="animate-spin text-blue-500" size={32} />
                        <span className="ml-2 text-gray-600">데이터를 불러오는 중...</span>
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full border-collapse">
                            <thead>
                            <tr className="bg-gray-100">
                                <th className="px-4 py-3 text-left font-semibold text-gray-700 border-b-2 border-gray-200 w-32">이름</th>
                                {vacationTypes.map(type => (
                                    <th key={type} className="px-4 py-3 text-center font-semibold text-gray-700 border-b-2 border-gray-200">
                                        {vacationTypeMap[type]}
                                    </th>
                                ))}
                                <th className="px-4 py-3 text-center font-semibold text-gray-700 border-b-2 border-gray-200 w-24"></th>
                            </tr>
                            </thead>
                            <tbody>
                            {vacationData.map((emp, index) => (
                                <tr key={emp.id} className={index % 2 === 0 ? "bg-white" : "bg-gray-50"}>
                                    <td className="px-4 py-3 border-b border-gray-200 font-medium">{emp.name}</td>
                                    {vacationTypes.map(type => {
                                        const info = emp.vacationInfos.find(
                                            v => v.vacationType === type || v.type === type
                                        );
                                        return (
                                            <td key={type} className="px-4 py-3 border-b border-gray-200 text-center">
                                                {info ? (
                                                    <input
                                                        type="number"
                                                        value={info.totalCount}
                                                        onChange={e => handleCountChange(emp.id, info.id, e.target.value)}
                                                        className="w-20 px-3 py-1 text-center border border-gray-300 rounded focus:ring-2 focus:ring-blue-400 focus:border-blue-500"
                                                        min="0"
                                                        step="0.5"
                                                    />
                                                ) : (
                                                    <span className="text-gray-400">-</span>
                                                )}
                                            </td>
                                        );
                                    })}
                                    <td className="px-4 py-3 border-b border-gray-200 text-center">
                                        <button
                                            onClick={() => saveEmployee(emp)}
                                            className="bg-green-500 hover:bg-green-600 text-white px-3 py-1 rounded-md text-sm transition-colors duration-200"
                                        >
                                            저장
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {vacationData.length === 0 && (
                                <tr>
                                    <td colSpan={vacationTypes.length + 2} className="px-4 py-8 text-center text-gray-500 border-b border-gray-200">
                                        검색 결과가 없습니다.
                                    </td>
                                </tr>
                            )}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* 페이지네이션 */}
                {!loading && vacationData.length > 0 && (
                    <div className="flex justify-center items-center gap-4 mt-6">
                        <button
                            onClick={() => handlePageChange(currentPage - 1)}
                            disabled={currentPage === 0}
                            className="flex items-center px-3 py-2 bg-white border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:bg-gray-100 transition-colors duration-200"
                        >
                            <ChevronLeft size={16} />
                            <span className="ml-1 font-medium">이전</span>
                        </button>

                        <div className="flex items-center">
              <span className="px-3 py-1 bg-blue-100 text-blue-700 font-medium rounded-md">
                {currentPage + 1}
              </span>
                            <span className="mx-2 text-gray-600">of</span>
                            <span className="text-gray-700">{totalPages}</span>
                        </div>

                        <button
                            onClick={() => handlePageChange(currentPage + 1)}
                            disabled={currentPage >= totalPages - 1}
                            className="flex items-center px-3 py-2 bg-white border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:bg-gray-100 transition-colors duration-200"
                        >
                            <span className="mr-1 font-medium">다음</span>
                            <ChevronRight size={16} />
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}