package org.joinmastodon.android.fragments;

public class test{

	@Override
	protected void doLoadData(int offset, int count) {
		List<Status> combinedData = new ArrayList<>();

		// Fetch home timeline
		AccountSessionManager.getInstance()
				.getAccount(accountID).getCacheController()
				.getHomeTimeline(offset > 0 ? maxID : null, count, refreshing, new SimpleCallback<CacheablePaginatedResponse<List<Status>>>(this) {
					@Override
					public void onSuccess(CacheablePaginatedResponse<List<Status>> result) {
						if (getActivity() == null || listMode != ListMode.FOLLOWING)
							return;
						if (refreshing)
							list.scrollToPosition(0);
						combinedData.addAll(result.items);
						maxID = result.maxID;

						// After fetching home timeline, fetch local timeline
						fetchLocalTimeline(combinedData, offset, count);
					}

					@Override
					public void onError(ErrorResponse error) {
						if (listMode != ListMode.FOLLOWING)
							return;
						super.onError(error);
					}
				});
	}

	private void fetchLocalTimeline(List<Status> combinedData, int offset, int count) {
		currentRequest = new GetPublicTimeline(true, false, offset > 0 ? maxID : null, null, count, null)
				.setCallback(new SimpleCallback<List<Status>>(this) {
					@Override
					public void onSuccess(List<Status> result) {
						combinedData.addAll(result);
						maxID = result.isEmpty() ? null : result.get(result.size() - 1).id;
						AccountSessionManager.get(accountID).filterStatuses(result, FilterContext.PUBLIC);

						// After fetching local timeline, combine and sort the data
						combineAndSortFeeds(combinedData);
					}

					@Override
					public void onError(ErrorResponse error) {
						currentRequest = null;
					}
				})
				.exec(accountID);
	}

	private void combineAndSortFeeds(List<Status> combinedData) {
		// Sort combined data by timestamp or any other criteria
		Collections.sort(combinedData, new Comparator<Status>() {
			@Override
			public int compare(Status s1, Status s2) {
				return s2.createdAt.compareTo(s1.createdAt); // Sort by descending order of creation time
			}
		});

		// Filter the content of each status
		for (Status status : combinedData) {
			status.content = filterWords(status.content);
		}

		// Pass combined and sorted data to the adapter
		onDataLoaded(combinedData, !combinedData.isEmpty());
	}

}
