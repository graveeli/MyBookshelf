package com.kunfei.bookshelf.view.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hwangjr.rxbus.thread.EventThread;
import com.kunfei.basemvplib.impl.IPresenter;
import com.kunfei.bookshelf.R;
import com.kunfei.bookshelf.base.MBaseFragment;
import com.kunfei.bookshelf.bean.BookContentBean;
import com.kunfei.bookshelf.bean.BookShelfBean;
import com.kunfei.bookshelf.bean.BookmarkBean;
import com.kunfei.bookshelf.bean.OpenChapterBean;
import com.kunfei.bookshelf.help.RxBusTag;
import com.kunfei.bookshelf.view.activity.ChapterListActivity;
import com.kunfei.bookshelf.view.adapter.ChapterListAdapter;
import com.kunfei.bookshelf.widget.recycler.scroller.FastScrollRecyclerView;

import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ChapterListFragment extends MBaseFragment {
    @BindView(R.id.rv_list)
    FastScrollRecyclerView rvList;
    @BindView(R.id.tv_current_chapter_info)
    TextView tvChapterInfo;
    @BindView(R.id.iv_chapter_sort)
    ImageView ivChapterSort;
    @BindView(R.id.ll_chapter_base_info)
    View llBaseInfo;
    @BindView(R.id.v_shadow)
    View vShadow;

    private Unbinder unbinder;

    private ChapterListAdapter chapterListAdapter;

    private LinearLayoutManager layoutManager;

    private BookShelfBean bookShelf;

    private boolean isChapterReverse;

    @Override
    public int createLayoutId() {
        return R.layout.fragment_chapter_list;
    }

    /**
     * P层绑定   若无则返回null;
     */
    @Override
    protected IPresenter initInjector() {
        return null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RxBus.get().register(this);
    }

    /**
     * 数据初始化
     */
    @Override
    protected void initData() {
        super.initData();
        bookShelf = ((ChapterListActivity) getActivity()).getBookShelf();
        isChapterReverse = preferences.getBoolean("isChapterReverse", false);
    }

    /**
     * 控件绑定
     */
    @Override
    protected void bindView() {
        super.bindView();
        unbinder = ButterKnife.bind(this, view);
        rvList.setLayoutManager(layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, isChapterReverse));
        rvList.setItemAnimator(null);
        chapterListAdapter = new ChapterListAdapter(bookShelf, new ChapterListAdapter.OnItemClickListener() {
            @Override
            public void itemClick(int index, int page, int tabPosition) {
                ((ChapterListActivity) getActivity()).searchViewCollapsed();
                if (index != bookShelf.getDurChapter()) {
                    RxBus.get().post(RxBusTag.SKIP_TO_CHAPTER, new OpenChapterBean(index, page));
                }
                getActivity().finish();
            }

            @Override
            public void itemLongClick(BookmarkBean bookmarkBean, int tabPosition) {
                ((ChapterListActivity) getActivity()).searchViewCollapsed();
                RxBus.get().post(RxBusTag.OPEN_BOOK_MARK, bookmarkBean);
                getActivity().finish();
            }
        });
        if (bookShelf != null) {
            rvList.setAdapter(chapterListAdapter);
            updateIndex(bookShelf.getDurChapter());
            updateChapterInfo();
        }
    }

    /**
     * 事件触发绑定
     */
    @Override
    protected void bindEvent() {
        super.bindEvent();
        tvChapterInfo.setOnClickListener(view -> layoutManager.scrollToPositionWithOffset(bookShelf.getDurChapter(), 0));

        ivChapterSort.setOnClickListener(v -> {
            if (chapterListAdapter != null) {
                isChapterReverse = !isChapterReverse;
                preferences.edit().putBoolean("isChapterReverse", isChapterReverse).apply();
                layoutManager.setReverseLayout(isChapterReverse);
                layoutManager.scrollToPositionWithOffset(bookShelf.getDurChapter(), 0);
            }
        });
    }

    private void updateIndex(int durChapter) {
        chapterListAdapter.setIndex(durChapter);
        layoutManager.scrollToPositionWithOffset(durChapter, 0);
    }

    private void updateChapterInfo() {
        if (bookShelf != null) {
            if (chapterListAdapter.getItemCount() == 0) {
                tvChapterInfo.setText(bookShelf.getDurChapterName());
            } else {
                tvChapterInfo.setText(String.format(Locale.getDefault(), "%s (%d/%d章)", bookShelf.getDurChapterName(), bookShelf.getDurChapter() + 1, bookShelf.getChapterListSize()));
            }
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        RxBus.get().unregister(this);
    }

    @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(RxBusTag.CHAPTER_CHANGE)})
    public void chapterChange(BookContentBean bookContentBean) {
        if (bookShelf != null && bookShelf.getNoteUrl().equals(bookContentBean.getNoteUrl())) {
            chapterListAdapter.upChapter(bookContentBean.getDurChapterIndex());
        }
    }
}